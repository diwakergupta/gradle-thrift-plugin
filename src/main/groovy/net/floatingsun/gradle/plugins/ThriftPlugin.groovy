package net.floatingsun.gradle.plugins;

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile

class ThriftExtension {
    String compiler = "thrift"
    String generator = "java:hashcode,beans"
}

class ThriftCompile extends AbstractCompile {
    protected void compile() {
        getDestinationDir().mkdir()

        getSource().getFiles().each { File file ->
            def cmd = [project.thrift.compiler, "--gen",
                project.thrift.generator, "-out", getDestinationDir()]
            cmd += file
            logger.info(cmd.toString())

            def output = new StringBuffer()
            Process result = cmd.execute()
            result.consumeProcessOutput(output, output)
            result.waitFor()
            if (result.exitValue() != 0) {
                throw new InvalidUserDataException(output.toString())
            }
        }
    }
}

public class ThriftPlugin implements Plugin<Project> {
    void apply(final Project project) {
        project.apply plugin: 'java'
        project.extensions.create('thrift', ThriftExtension)

        project.sourceSets.all { SourceSet sourceSet ->
            ThriftCompile generateJavaTask = project.tasks.add(
                sourceSet.getTaskName('generate', 'thrift'), ThriftCompile)
            generateJavaTask.setClasspath(sourceSet.getCompileClasspath())
            generateJavaTask.setDestinationDir(project.file(
                    "$project.buildDir/generated-sources/$sourceSet.name"))

            def final thriftSourceSet = new DefaultSourceDirectorySet(
                "${sourceSet.displayName}Thrift", project.fileResolver)
            thriftSourceSet.include("**/*.thrift")
            // thriftSourceSet.filter.include("**/*.thrift")
            thriftSourceSet.srcDir("src/${sourceSet.name}/thrift")
            println thriftSourceSet.getSrcDirs()
            generateJavaTask.setSource(thriftSourceSet)

            sourceSet.java.srcDir "${project.buildDir}/generated-sources/${sourceSet.name}"
            String compileJavaTaskName = sourceSet.getCompileTaskName("java");
            Task compileJavaTask = project.tasks.getByName(compileJavaTaskName);
            compileJavaTask.dependsOn(generateJavaTask)
        }
    }
}
