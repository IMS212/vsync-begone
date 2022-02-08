import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.coolcrabs.brachyura.compiler.java.JavaCompilation;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.fabric.FabricProject;
import io.github.coolcrabs.brachyura.ide.IdeProject;
import io.github.coolcrabs.brachyura.mappings.Namespaces;
import io.github.coolcrabs.brachyura.mappings.tinyremapper.MappingTreeMappingProvider;
import io.github.coolcrabs.brachyura.mappings.tinyremapper.RemapperProcessor;
import io.github.coolcrabs.brachyura.processing.ProcessingEntry;
import io.github.coolcrabs.brachyura.processing.ProcessorChain;
import io.github.coolcrabs.brachyura.processing.sinks.AtomicZipProcessingSink;
import io.github.coolcrabs.brachyura.processing.sources.DirectoryProcessingSource;
import io.github.coolcrabs.brachyura.processing.sources.ProcessingSponge;
import io.github.coolcrabs.brachyura.util.JvmUtil;
import io.github.coolcrabs.brachyura.util.Lazy;
import io.github.coolcrabs.brachyura.util.OsUtil;
import io.github.coolcrabs.brachyura.util.PathUtil;
import io.github.coolcrabs.brachyura.util.Util;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.TinyRemapper;

public abstract class MultiSrcDirFabricProject extends FabricProject {
  public abstract List<Path> getHeaderPaths(String subdir);
  public abstract List<Path> getCompilePaths(String subdir);
  public abstract List<String> getClientVMArgs();
  public abstract List<String> getServerVMArgs();
  
  @Override
  public JavaJarDependency build() {
    try {
      String mixinOut = "mixinmapout.tiny";
      JavaCompilation compilation = new JavaCompilation()
        .addOption(JvmUtil.compileArgs(JvmUtil.CURRENT_JAVA_VERSION, this.getJavaVersion()))
        .addOption(
          "-AbrachyuraInMap=" + this.writeMappings4FabricStuff().toAbsolutePath().toString(),
          "-AbrachyuraOutMap=" + mixinOut, // Remaps shadows etc
          "-AbrachyuraInNamespace=" + Namespaces.NAMED,
          "-AbrachyuraOutNamespace=" + Namespaces.INTERMEDIARY,
          "-AoutRefMapFile=" + this.getModId() + "-refmap.json", // Remaps annotations
          "-AdefaultObfuscationEnv=brachyura"
        )
        .addClasspath(this.getCompileDependencies());
      
      for (Path p : this.getCompilePaths("java")) {
        compilation.addSourceDir(p);
      }
      
      for (Path p : this.getHeaderPaths("java")) {
        compilation.addSourcePathDir(p);
      }
      
      ProcessingSponge compilationOutput = new ProcessingSponge();
      compilation.compile().getInputs(compilationOutput);
      MemoryMappingTree compmappings = new MemoryMappingTree(true);
      mappings.get().accept(new MappingSourceNsSwitch(compmappings, Namespaces.NAMED));
      ProcessingSponge trout = new ProcessingSponge();
      ProcessingEntry mixinMappings = compilationOutput.popEntry(mixinOut);
      
      if (mixinMappings != null) {
        try (Reader reader = new InputStreamReader(mixinMappings.in.get())) {
          // For easier debugging a seperate tree is made here
          MemoryMappingTree mixinMappingsTree = new MemoryMappingTree();
          MappingReader.read(reader, MappingFormat.TINY_2, mixinMappingsTree);
          mixinMappingsTree.accept(compmappings);
        }
      }
      
      new ProcessorChain(
        new RemapperProcessor(TinyRemapper.newRemapper()
              .withMappings(new MappingTreeMappingProvider(compmappings, 
                                                           Namespaces.NAMED, 
                                                           Namespaces.INTERMEDIARY)), 
              this.getCompileDependencies())
      ).apply(trout, compilationOutput);
      
      try (AtomicZipProcessingSink out = new AtomicZipProcessingSink(this.getBuildJarPath())) {
        List<Path> resources = this.getCompilePaths("resources");
        DirectoryProcessingSource[] sources = new DirectoryProcessingSource[resources.size()];
        
        for (int i = 0; i < resources.size(); i++)
          sources[i] = new DirectoryProcessingSource(resources.get(i));
        
        this.resourcesProcessingChain().apply(out, sources);
        trout.getInputs(out);
        out.commit();
      }
      
      return new JavaJarDependency(this.getBuildJarPath(), null, this.getId());
    } catch (Exception e) {
      throw Util.sneak(e);
    }
  }
  
  @Override
  public IdeProject getIdeProject() {
    Path cwd = PathUtil.resolveAndCreateDir(this.getProjectDir(), "run");
    Lazy<List<Path>> classpath = new Lazy<>(() -> {
      Path mappingsClasspath = this.writeMappings4FabricStuff().getParent().getParent();
      ArrayList<Path> r = new ArrayList<>(this.runtimeDependencies.get().size() + 1);
      
      for (JavaJarDependency dependency : this.runtimeDependencies.get()) {
        r.add(dependency.jar);
      }
      
      r.add(mappingsClasspath);
      return r;
    });
    
    Lazy<Path> launchConfig = new Lazy<>(this::writeLaunchCfg);
    List<Path> paths = new ArrayList<>(this.getCompilePaths("java"));
    paths.addAll(this.getHeaderPaths("java"));
    
    return new IdeProject.IdeProjectBuilder()
      .name(this.getModId())
      .javaVersion(getJavaVersion())
      .dependencies(this.ideDependencies)
      .sourcePaths(paths)
      // TODO: Audit runRunConfig behavior in BaseJavaProject - mixed references to IdeProject and RunConfig
      .resourcePaths(this.getCompilePaths("resources"))
      .runConfigs(
        // Client config
        new IdeProject.RunConfig.RunConfigBuilder()
          .name("Minecraft Client")
          .cwd(cwd)
          .mainClass("net.fabricmc.devlaunchinjector.Main")
          .classpath(classpath)
          .resourcePaths(this.getCompilePaths("resources"))
          .vmArgs(
            () -> {
              ArrayList<String> clientArgs = new ArrayList<>(Arrays.asList(
                "-Dfabric.dli.config=" + launchConfig.get().toString(),
                "-Dfabric.dli.env=client",
                "-Dfabric.dli.main=net.fabricmc.loader.launch.knot.KnotClient"
              ));
              clientArgs.addAll(this.getClientVMArgs());
              
              if (OsUtil.OS == OsUtil.Os.OSX) {
                clientArgs.add("-XstartOnFirstThread");
              }
              return clientArgs;
            }
          )
          .build(),
        
        // Server config
        new IdeProject.RunConfig.RunConfigBuilder()
          .name("Minecraft Server")
          .cwd(cwd)
          .mainClass("net.fabricmc.devlaunchinjector.Main")
          .classpath(classpath)
          .resourcePaths(this.getResourcesDir())
          .vmArgs(
            () -> {
              ArrayList<String> serverArgs = new ArrayList<>(Arrays.asList(
                "-Dfabric.dli.config=" + launchConfig.get().toString(),
                "-Dfabric.dli.env=client",
                "-Dfabric.dli.main=net.fabricmc.loader.launch.knot.KnotClient"
              ));
              serverArgs.addAll(this.getServerVMArgs());
              
              return serverArgs;
            }
          )
          .build()
      )
      .build();
  }
}
