package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.junit.Rule

class SubTemplatesFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()

    void setup() {
        def proxyAddress = recorder.proxy.address()
        env["JAVA_OPTS"] = "-Dhttps.proxyHost=" + proxyAddress.hostName + " -Dhttps.proxyPort=" + proxyAddress.port +
                " -Dhttp.proxyHost=" + proxyAddress.hostName + " -Dhttp.proxyPort=" + proxyAddress.port
    }

    def "Generate command installs a sub-template"() {
        given: "A new project created from a Lazybones template"
        def appDir = createProjectWithSubTemplates()

        when: "I use the generate command within the new project"
        def exitCode = runCommand(["generate", "controller"], appDir, ["org.example", "Book"], false)

        then: "It creates a new controller file in the correct location and with correct name"
        exitCode == 0

        def controllerFile = new File(appDir, "src/main/groovy/org/example/BookController.groovy")
        controllerFile.exists()
        controllerFile.text =~ /class BookController /

        and: "The output declares the file was created"
        output =~ "Created new controller src/main/groovy/org/example/BookController.groovy"

        and: "The unpacked template is deleted"
        !new File(appDir, ".lazybones/controller-unpacked").exists()
    }

    def "Generate command passes project template parameters to sub-template"() {
        given: "A new project created from a Lazybones template"
        def appDir = createProjectWithSubTemplates()

        when: "I use the generate command within the new project"
        def exitCode = runCommand(["generate", "entity"], appDir, ["org.example", "Book"], false)

        then: "It creates a new controller file in the correct location and with correct name"
        exitCode == 0

        def entityFile = new File(appDir, "src/main/groovy/org/example/Book.groovy")
        entityFile.exists()
        entityFile.text =~ /Entity\(group="foo", version="0.1"\)/
        entityFile.text =~ /class Book /

        and: "The output declares the file was created"
        output =~ "Created new persistence entity src/main/groovy/org/example/Book.groovy"

        and: "The unpacked template is deleted"
        !new File(appDir, ".lazybones/entity-unpacked").exists()
    }

    def "Generate command passes command qualifiers to post-install script"() {
        given: "A new project created from a Lazybones template"
        def appDir = createProjectWithSubTemplates()

        when: "I use the generate command within the new project with a set of qualifiers"
        def exitCode = runCommand(["generate", "controller::one::apple::shoe"], appDir, ["org.example", "Book"], false)

        then: "The command succeeds"
        exitCode == 0

        and: "The qualifiers are passed to the post-install script as a list"
        output =~ "Found command qualifiers: \\[one, apple, shoe\\]"
    }

    def "Generate command fails gracefully when sub-template not found"() {
        given: "A new project created from a Lazybones template"
        def appDir = createProjectWithSubTemplates()

        when: "I run the generate command with a template that doesn't exist"
        def exitCode = runCommand(["generate", "unknown"], appDir, [], false)

        then: "The command exits with an error code"
        exitCode == 1

        and: "It prints a warning, but no exception"
        output =~ "Cannot find a sub-template named 'unknown'"
        !(output =~ "Exception")
    }

    def "Generate command fails gracefully when sub-template post-install script throws exception"() {
        given: "A new project created from a Lazybones template"
        def appDir = createProjectWithSubTemplates()

        when: "I run the generate command with a bad template"
        def exitCode = runCommand(["generate", "bad"], appDir, [], false)

        then: "The command exits with an error code"
        exitCode == 1

        and: "It prints a warning, but no exception"
        output =~ "Post install script caused an exception, project might be corrupt: Just a test error"
        !(output =~ "Exception")
    }

    def "Generate command displays exception from post-install script with --stacktrace"() {
        given: "A new project created from a Lazybones template"
        def appDir = createProjectWithSubTemplates()

        when: "I run the generate command with a bad template"
        def exitCode = runCommand(["--stacktrace", "generate", "bad"], appDir, [], false)

        then: "The command exits with an error code"
        exitCode == 1

        and: "It prints a warning, but no exception"
        output =~ "Post install script caused an exception, project might be corrupt: Just a test error"
        output =~ "FileNotFoundException: Just a test error"
    }

    def "Generate command fails gracefully when invoked inside a non-Lazybones project directory"() {
        given: "A new directory that is not a project created by Lazybones"
        def appDir = new File(baseWorkDir, "otherapp")
        appDir.mkdirs()

        when: "I run the generate command with a bad template"
        def exitCode = runCommand(["generate", "bad"], appDir, [], false)

        then: "The command exits with an error code"
        exitCode == 1

        and: "It prints a warning, but no exception"
        output =~ "You cannot use `generate` here: this is not a Lazybones-created project"
        !(output =~ "Exception")
    }

    protected File createProjectWithSubTemplates() {
        def appDir = new File(baseWorkDir, "maa")
        assert runCommand(
                ["create", "subtemplates-tmpl", "0.1", appDir.name, "-Pgroup=foo", "-Pversion=0.1"],
                appDir.parentFile) == 0
        assert appDir.exists()
        assert new File(appDir, "build.gradle").exists()

        return appDir
    }
}
