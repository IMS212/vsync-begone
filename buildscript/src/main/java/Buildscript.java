import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.fabric.FabricLoader;
import io.github.coolcrabs.brachyura.fabric.FabricMaven;
import io.github.coolcrabs.brachyura.mappings.Namespaces;
import io.github.coolcrabs.brachyura.processing.ProcessorChain;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerVisitor;
import net.fabricmc.mappingio.tree.MappingTree;

public class Buildscript extends SingleSrcDirFabricProject {
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
  
  // JVM flags for each minecraft server and minecraft client
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
  
  ////////////////////////////////////////////
  // You rarely need modify part below here //
  ////////////////////////////////////////////
  @Override
  public Consumer<AccessWidenerVisitor> getAw() {
    return v -> {
      Path path = this.getResourcesDir().resolve(this.getModId() + ".accesswidener");
      try {
        new AccessWidenerReader(v).read(
          Files.newBufferedReader(
            path
          ), 
          Namespaces.NAMED
        );
      } catch (IOException e) {
        // Assuming NoSuchFileException mea the writer dont need AW
        if (e instanceof NoSuchFileException) {
          Logger.warn(String.format("Can't find accesswidener at %s (assumming you dont use access wideners or ignore if you dont know what access widener is)", path.toString()));
        } else {
          throw new UncheckedIOException(e);
        }
      }
    };
  }
  
  @Override
  public Path getBuildJarPath() {
    return this.getBuildLibsDir().resolve(this.getModId() + "-" + this.getVersion() + "-" + this.getReleaseType() + ".jar");
  }
  
  @Override
  public ProcessorChain resourcesProcessingChain() {
    return new ProcessorChain(super.resourcesProcessingChain(), 
                              new FmjVersionFixer(this));
  }
}

