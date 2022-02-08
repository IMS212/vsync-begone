import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.nio.file.NoSuchFileException;

import io.github.coolcrabs.brachyura.compiler.java.JavaCompilation;
import io.github.coolcrabs.brachyura.compiler.java.JavaCompilationResult;
import io.github.coolcrabs.brachyura.dependency.JavaJarDependency;
import io.github.coolcrabs.brachyura.fabric.FabricLoader;
import io.github.coolcrabs.brachyura.fabric.FabricMaven;
import io.github.coolcrabs.brachyura.ide.IdeProject;
import io.github.coolcrabs.brachyura.ide.IdeProject.RunConfig;
import io.github.coolcrabs.brachyura.mappings.Namespaces;
import io.github.coolcrabs.brachyura.maven.MavenId;
import io.github.coolcrabs.brachyura.processing.ProcessorChain;
import io.github.coolcrabs.brachyura.processing.sinks.DirectoryProcessingSink;
import io.github.coolcrabs.brachyura.project.Task;
import io.github.coolcrabs.brachyura.util.JvmUtil;
import io.github.coolcrabs.brachyura.util.Util;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;
import net.fabricmc.mappingio.tree.MappingTree;

public class Buildscript extends MultiSrcDirFabricProject {
  // Version which your mod targetting
  @Override
  public String getMcVersion() {
    return "1.18.1";
  }
  
  // Release type for your mod (can be any string)
  public String getReleaseType() {
    return "dev";
  }
  
  @Override
  public MappingTree createMappings() {
    // Mojmap
    //return Mappings.createMojmap(Mappings.createIntermediary(this.getMcVersion()), this.getMcVersion());
    
    // Yarn
    return Mappings.createYarn(this.getMcVersion() + "+build.7");
  }
  
  // Select fabric loader which your mod uses
  @Override
  public FabricLoader getLoader() {
    return new FabricLoader(FabricMaven.URL, FabricMaven.loader("0.12.12"));
  }
  
  // Your mod version
  @Override
  public String getVersion() {
    return "1.0.0";
  }
  
  @Override
  public void getModDependencies(ModDependencyCollector deps) {
    // Example of adding new deps
    //deps.addMaven(FabricMaven.URL, 
    //           new MavenId("net.fabricmc.fabric-api:fabric-api:0.44.0+1.18"), 
    //           ModDependencyFlag.COMPILE, 
    //           ModDependencyFlag.RUNTIME);
    // ModDependencyFlag.JIJ is jar in jar
  }
  
  // The java version your mod be compiled with
  // Generally should be same as java version needed by 
  // Minecraft you are targetting
  @Override
  public int getJavaVersion() {
    // 1.18 needs Java 17
    return 17;
  }
  
  ////////////////////////////////////////////
  // You rarely need modify part below here //
  ////////////////////////////////////////////
  @Override
  public Path getSrcDir() {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public Consumer<AccessWidenerVisitor> getAw() {
    return v -> {
      try {
        new AccessWidenerReader(v).read(
          Files.newBufferedReader(
            this.getResourcesDir().resolve(this.getModId() + ".accesswidener")
          ), 
          Namespaces.NAMED
        );
      } catch (IOException e) {
        // Only if its not NoSuchFileException
        // because its can be just the mod writer dont need AW
        if (!(e instanceof NoSuchFileException)) {
          throw new UncheckedIOException(e);
        }
      }
    };
  }
  
  @Override
  public List<Path> getHeaderPaths(String subdir) {
    List<Path> r = new ArrayList<>();
    // Append header here if you dont know ignore this im also dont know :)
    return r;
  }
  
  @Override
  public List<Path> getCompilePaths(String subdir) {
    List<Path> r = new ArrayList<>();
    
    r.add(this.getProjectDir().resolve("src")
                              .resolve("main")
                              .resolve(subdir));
    
    for (int i = 0; i < r.size(); i++) 
      if (!Files.exists(r.get(i))) 
        r.remove(i);
    
    return r;
  }
  
  public List<String> getClientVMArgs() {
    List<String> args = new ArrayList<>();
    
    // My args for 1 GB heap
    args.add("-XX:+IdleTuningGcOnIdle");
    args.add("-XX:+UseAggressiveHeapShrink");
    args.add("-XX:-OmitStackTraceInFastThrow");
    args.add("-XX:+UseFastAccessorMethods");
    args.add("-XX:+OptimizeStringConcat");
    args.add("-Xshareclasses:allowClasspaths");
    args.add("-Xaot");
    args.add("-XX:+UseCompressedOops");
    args.add("-XX:ObjectAlignmentInBytes=256");
    args.add("-Xshareclasses");
    args.add("-XX:SharedCacheHardLimit=800M");
    args.add("-Xscmx800M");
    args.add("-Xtune:virtualized");
    args.add("-XX:+TieredCompilation");
    args.add("-XX:InitialTenuringThreshold=5");
    args.add("-Dlog4j2.formatMsgNoLookups=true");
    args.add("-XX:-DisableExplicitGC");
    args.add("-XX:InitiatingHeapOccupancyPercent=35");
    args.add("-XX:+UnlockExperimentalVMOptions");
    args.add("-XX:+UseG1GC");
    args.add("-XX:MaxGCPauseMillis=6");
    args.add("-Djava.net.preferIPv4Stack=true");
    args.add("-XX:-ParallelRefProcEnabled");
    args.add("-XX:+UseTLAB");
    args.add("-Xmn100M");
    args.add("-Xmx1G");
    args.add("-Xms101M");
    args.add("-XX:ReservedCodeCacheSize=70M");
    args.add("-XX:G1NewSizePercent=20");
    args.add("-XX:G1ReservePercent=20");
    args.add("-XX:ParallelGCThreads=2");
    args.add("-XX:ConcGCThreads=1");
    
    return args;
  }
  
  public List<String> getServerVMArgs() {
    List<String> args = new ArrayList<>();
    
    // My args for 1 GB heap
    args.add("-XX:+IdleTuningGcOnIdle");
    args.add("-XX:+UseAggressiveHeapShrink");
    args.add("-XX:-OmitStackTraceInFastThrow");
    args.add("-XX:+UseFastAccessorMethods");
    args.add("-XX:+OptimizeStringConcat");
    args.add("-Xshareclasses:allowClasspaths");
    args.add("-Xaot -XX:+UseCompressedOops");
    args.add("-XX:ObjectAlignmentInBytes=256");
    args.add("-Xshareclasses");
    args.add("-XX:SharedCacheHardLimit=800M");
    args.add("-Xscmx800M");
    args.add("-Xtune:virtualized");
    args.add("-XX:+TieredCompilation");
    args.add("-XX:InitialTenuringThreshold=5");
    args.add("-Dlog4j2.formatMsgNoLookups=true");
    args.add("-XX:-DisableExplicitGC");
    args.add("-XX:InitiatingHeapOccupancyPercent=35");
    args.add("-XX:+UnlockExperimentalVMOptions");
    args.add("-XX:+UseG1GC");
    args.add("-XX:MaxGCPauseMillis=6");
    args.add("-Djava.net.preferIPv4Stack=true");
    args.add("-XX:-ParallelRefProcEnabled");
    args.add("-XX:+UseTLAB");
    args.add("-Xmn100M");
    args.add("-Xmx1G");
    args.add("-Xms101M");
    args.add("-XX:ReservedCodeCacheSize=70M");
    args.add("-XX:G1NewSizePercent=20");
    args.add("-XX:G1ReservePercent=20");
    args.add("-XX:ParallelGCThreads=2");
    args.add("-XX:ConcGCThreads=1");
    
    return args;
  }
  
  @Override
  public Path getBuildJarPath() {
    return getBuildLibsDir().resolve(getModId() + "-" + getVersion() + "-" + getReleaseType() + ".jar");
  }
  
  @Override
  public ProcessorChain resourcesProcessingChain() {
    return new ProcessorChain(super.resourcesProcessingChain(), 
                              new FmjVersionFixer(this));
  }
}

