package io.anuke.arc.setup;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.setup.DependencyBank.ProjectDependency;
import io.anuke.arc.setup.DependencyBank.ProjectType;

import java.io.*;
import java.util.*;

public class ArcSetup{
    public Array<ProjectType> modules = new Array<>();
    public Array<ProjectDependency> dependencies = new Array<>();
    public String template, outputDir, appName, packageName, mainClass, sdkLocation;
    public Consumer<String> callback = System.out::print;
    public Array<String> gradleArgs = new Array<>();

    public void build(){
        Project project = new Project();

        String packageDir = packageName.replace('.', '/');
        String sdkPath = sdkLocation.replace('\\', '/');

        if(!isSdkLocationValid(sdkLocation)){
            callback.accept("Android SDK location '" + sdkLocation + "' doesn't contain an SDK!\n");
        }

        FileHandle tmpSettings = Core.files.external(".tmp/arc-setup-settings.gradle");
        FileHandle tmpBuild = Core.files.external(".tmp/arc-setup-build.gradle");
        tmpSettings.writeString("include " + modules.map(type -> "'" + type.name() + "'").toString(", ") + "\n" + Core.files.internal("templates/base/settings.gradle").readString());

        String ptemplate = Core.files.internal("templates/base/project_template").readString();

        StringBuilder buildString = new StringBuilder(Core.files.internal("templates/base/build.gradle").readString()
            .replace("%CLASSPATH_PLUGINS%", modules.map(type -> type.classpathPlugin).reduce("", (name, str) -> name == null ? str : str + "classpath '" + name + "'\n")));

        for(ProjectType ptype : modules){
            buildString.append(ptemplate
                .replace("%PLUGINS%", Array.with(ptype.plugins).reduce("", (plugin, str) -> str + "\napply plugin: \"" + plugin + "\"")
                .replace("%DEPENDENCIES%", (ptype == ProjectType.core ? "" : "implementation project(\":core\")")  +
                    dependencies.reduce("", (dependency, str) -> !dependency.dependencies.containsKey(ptype) ? str :
                    Array.with(dependency.dependencies.get(ptype)).reduce("", (bstr, dep) -> "\n" + bstr +
                    ((ptype == ProjectType.android && dep.contains("native")) ? "natives \"" + dep + "\"" :
                    dep.contains("arc ") ? "implementation arcModule(\"" + dep.replace("arc ", "") + "\")" : "implementation \"" + dep + "\""))))));
        }

        tmpBuild.writeString(buildString.toString());

        String template = "base";

        // root dir/gradle files
        project.files.add(new ProjectFile(template, "gitignore", ".gitignore", false));
        project.files.add(new TemporaryProjectFile(template, tmpSettings.file(), "settings.gradle", false));
        project.files.add(new TemporaryProjectFile(template, tmpBuild.file(), "build.gradle", true));
        project.files.add(new ProjectFile(template, "gradlew", false));
        project.files.add(new ProjectFile(template, "gradlew.bat", false));
        project.files.add(new ProjectFile(template, "gradle/wrapper/gradle-wrapper.jar", false));
        project.files.add(new ProjectFile(template, "gradle/wrapper/gradle-wrapper.properties", false));
        project.files.add(new ProjectFile(template, "gradle.properties"));

        // core project
        project.files.add(new ProjectFile(template, "core/build.gradle"));

        FileHandle src = Core.files.internal("templates/" + this.template + "/files");
        String[] files = src.readString().replace("\n", "").split(",");

        project.files.add(new ProjectFile(this.template, "MainClass", "core/src/" + packageDir + "/" + mainClass + ".java", true));

        for(String sourcefile : files){
            project.files.add(new ProjectFile(this.template, "" + sourcefile, "core/src/" + packageDir + "/" + sourcefile + ".java", true));
        }

        if(modules.contains(ProjectType.html)){
            project.files.add(new ProjectFile(template, "core/CoreGdxDefinition", "core/src/" + mainClass + ".gwt.xml", true));
        }

        String[] fileDirs = {"assets", "assets-raw"};

        for(String dir : fileDirs){
            dir = "templates/base/core/" + dir + "/files";
            FileHandle file = Core.files.internal(dir);
            String[] lines = file.readString().split("\n");

            for(String respath : lines){
                project.files.add(new ProjectFile(template, "core/" + respath, false));
            }
        }

        // desktop project
        if(modules.contains(ProjectType.desktop)){
            project.files.add(new ProjectFile(template, "desktop/build.gradle"));
            project.files.add(new ProjectFile(template, "desktop/src/DesktopLauncher", "desktop/src/" + packageDir + "/desktop/DesktopLauncher.java", true));
        }

        // Assets
        String assetPath = "core/assets";

        // android project
        if(modules.contains(ProjectType.android)){
            project.files.add(new ProjectFile(template, "android/res/values/strings.xml"));
            project.files.add(new ProjectFile(template, "android/res/values/styles.xml", false));
            project.files.add(new ProjectFile(template, "android/res/drawable-hdpi/ic_launcher.png", false));
            project.files.add(new ProjectFile(template, "android/res/drawable-mdpi/ic_launcher.png", false));
            project.files.add(new ProjectFile(template, "android/res/drawable-xhdpi/ic_launcher.png", false));
            project.files.add(new ProjectFile(template, "android/res/drawable-xxhdpi/ic_launcher.png", false));
            project.files.add(new ProjectFile(template, "android/res/drawable-xxxhdpi/ic_launcher.png", false));
            project.files.add(new ProjectFile(template, "android/src/AndroidLauncher", "android/src/" + packageDir + "/AndroidLauncher.java", true));
            project.files.add(new ProjectFile(template, "android/AndroidManifest.xml"));
            project.files.add(new ProjectFile(template, "android/build.gradle", true));
            project.files.add(new ProjectFile(template, "android/ic_launcher-web.png", false));
            project.files.add(new ProjectFile(template, "android/proguard-project.txt", false));
            project.files.add(new ProjectFile(template, "android/project.properties", false));
            project.files.add(new ProjectFile(template, "local.properties", true));
        }

        // html project
        if(modules.contains(ProjectType.html)){
            project.files.add(new ProjectFile(template, "html/build.gradle"));
            project.files.add(new ProjectFile(template, "html/src/HtmlLauncher", "html/src/" + packageDir + "/client/HtmlLauncher.java", true));
            project.files.add(new ProjectFile(template, "html/GdxDefinition", "html/src/" + packageDir + "/GdxDefinition.gwt.xml", true));
            project.files.add(new ProjectFile(template, "html/GdxDefinitionSuperdev", "html/src/" + packageDir + "/GdxDefinitionSuperdev.gwt.xml", true));
            project.files.add(new ProjectFile(template, "html/war/index", "html/webapp/index.html", true));
            project.files.add(new ProjectFile(template, "html/war/styles.css", "html/webapp/styles.css", false));
            project.files.add(new ProjectFile(template, "html/war/soundmanager2-jsmin.js", "html/webapp/soundmanager2-jsmin.js", false));
            project.files.add(new ProjectFile(template, "html/war/soundmanager2-setup.js", "html/webapp/soundmanager2-setup.js", false));
            project.files.add(new ProjectFile(template, "html/war/WEB-INF/web.xml", "html/webapp/WEB-INF/web.xml", true));
        }

        // ios robovm
        if(modules.contains(ProjectType.ios)){
            project.files.add(new ProjectFile(template, "ios/src/IOSLauncher", "ios/src/" + packageDir + "/IOSLauncher.java", true));
            project.files.add(new ProjectFile(template, "ios/data/Default.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Default@2x.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Default@2x~ipad.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Default-568h@2x.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Default~ipad.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Default-375w-667h@2x.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Default-414w-736h@3x.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Default-1024w-1366h@2x~ipad.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Icon.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Icon@2x.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Icon-72.png", false));
            project.files.add(new ProjectFile(template, "ios/data/Icon-72@2x.png", false));
            project.files.add(new ProjectFile(template, "ios/build.gradle", true));
            project.files.add(new ProjectFile(template, "ios/Info.plist.xml", false));
            project.files.add(new ProjectFile(template, "ios/robovm.properties"));
            project.files.add(new ProjectFile(template, "ios/robovm.xml", true));
        }

        Map<String, String> values = new HashMap<>();
        values.put("%APP_NAME%", appName);
        values.put("%APP_NAME_ESCAPED%", appName.replace("'", "\\'"));
        values.put("%PACKAGE%", packageName);
        values.put("%PACKAGE_DIR%", packageDir);
        values.put("%MAIN_CLASS%", mainClass);
        values.put("%ANDROID_SDK%", sdkPath);
        values.put("%ASSET_PATH%", assetPath);
        values.put("%BUILD_TOOLS_VERSION%", DependencyBank.buildToolsVersion);
        values.put("%API_LEVEL%", DependencyBank.androidAPILevel);
        values.put("%GWT_VERSION%", DependencyBank.gwtVersion);
        values.put("%GWT_INHERITS%", parseGwtInherits());

        copyAndReplace(outputDir, project, values);

        // HACK executable flag isn't preserved for whatever reason...
        new File(outputDir, "gradlew").setExecutable(true);

        tmpSettings.delete();
        tmpBuild.delete();

        execute(new File(outputDir), "gradlew.bat", "gradlew", "clean " + gradleArgs.toString(" "), callback);
    }

    private void copyAndReplace(String outputDir, Project project, Map<String, String> values){
        File out = new File(outputDir);
        if(!out.exists() && !out.mkdirs()){
            throw new RuntimeException("Couldn't create output directory '" + out.getAbsolutePath() + "'");
        }

        for(ProjectFile file : project.files){
            copyFile(file, out, values);
        }
    }

    private void copyFile(ProjectFile file, File out, Map<String, String> values){
        FileHandle outFile = new FileHandle(new File(out, file.outputName));

        if(!outFile.parent().exists()){
            outFile.parent().mkdirs();
        }

        boolean isTemp = file instanceof TemporaryProjectFile;

        if(file.isTemplate){
            String txt;
            if(isTemp){
                txt = new FileHandle(((TemporaryProjectFile)file).file).readString();
            }else{
                txt = new FileHandle(file.resourceName + file.resourceLoc).readString();
            }

            txt = replace(txt, values);
            outFile.writeString(txt);
        }else{
            Core.files.internal(file.resourceName + "/" + file.resourceLoc).copyTo(outFile);
        }
    }

    private String replace(String txt, Map<String, String> values){
        for(String key : values.keySet()){
            String value = values.get(key);
            txt = txt.replace(key, value);
        }
        return txt;
    }

    private String parseGwtInherits(){
        String parsed = "";

        for(ProjectDependency dep : dependencies){
            //TODO add gwt inherits: "\t<inherits name='" + inherit + "' />\n";
        }

        return parsed;
    }

    private boolean isSdkLocationValid(String sdkLocation){
        return new File(sdkLocation, "tools").exists() && new File(sdkLocation, "platforms").exists();
    }

    /**
     * Execute the file with the given parameters.
     * @return whether the execution succeeded
     */
    private static boolean execute(File workingDir, String windowsFile, String unixFile, String parameters, Consumer<String> callback){
        String exec = workingDir.getAbsolutePath() + "/" + (System.getProperty("os.name").contains("Windows") ? windowsFile : unixFile);
        String log = "Executing '" + exec + " " + parameters + "'";
        callback.accept(log);
        callback.accept("\n");

        String[] params = parameters.split(" ");
        String[] commands = new String[params.length + 1];
        commands[0] = exec;
        System.arraycopy(params, 0, commands, 1, params.length);

        try{
            final Process process = new ProcessBuilder(commands).redirectErrorStream(true).directory(workingDir).start();

            Thread t = new Thread(() -> {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1);
                try{
                    String result;
                    while((result = reader.readLine()) != null){
                        callback.accept(result);
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
            });
            t.setDaemon(true);
            t.start();
            process.waitFor();
            t.interrupt();
            return process.exitValue() == 0;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * A temporary file that wraps {@link ProjectFile} for use in a {@link Project}
     * @author Tomski
     */
    public static class TemporaryProjectFile extends ProjectFile{

        /** The temporary file **/
        public File file;

        public TemporaryProjectFile(String template, File file, String outputString, boolean isTemplate){
            super(template, outputString, isTemplate);
            this.file = file;
        }

    }

    /**
     * Describes all the files required to generate a
     * new project for an Application. Files are found
     * on the classpath of the gdx-setup project, see
     * package com.badlogic.gdx.setup.resources.
     * @author badlogic
     */
    public static class Project{
        /** list of files, relative to project directory **/
        public List<ProjectFile> files = new ArrayList<>();
    }

    /**
     * A file in a {@link Project}, the resourceName specifies the location
     * of the template file, the outputName specifies the final name of the
     * file relative to its project, the isTemplate field specifies if
     * values need to be replaced in this file or not.
     * @author badlogic
     */
    public static class ProjectFile{
        /** the name of the template resource, relative to resourceLoc **/
        public String resourceName;
        /** the name of the output file, including directories, relative to the project dir **/
        public String outputName;
        /** whether to replace values in this file **/
        public boolean isTemplate;
        /** If the resource is from resource directory, or working dir **/
        public String resourceLoc = "templates/";

        public ProjectFile(String template, String name){
            this.resourceName = name;
            this.outputName = name;
            this.isTemplate = true;
            resourceLoc += template + "/";
        }

        public ProjectFile(String template, String name, boolean isTemplate){
            this.resourceName = name;
            this.outputName = name;
            this.isTemplate = isTemplate;
            resourceLoc += template + "/";
        }

        public ProjectFile(String template, String resourceName, String outputName, boolean isTemplate){
            this.resourceName = resourceName;
            this.outputName = outputName;
            this.isTemplate = isTemplate;
            resourceLoc += template + "/";
        }
    }
}
