package uk.co.cacoethes.lazybones

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.junit.Rule

class SubGeneratorsFunctionalSpec extends AbstractFunctionalSpec {
    @Rule Recorder recorder = new Recorder()

    void setup() {
        def proxyAddress = recorder.proxy.address()
        env["JAVA_OPTS"] = "-Dhttps.proxyHost=" + proxyAddress.hostName + " -Dhttps.proxyPort=" + proxyAddress.port +
                " -Dhttp.proxyHost=" + proxyAddress.hostName + " -Dhttp.proxyPort=" + proxyAddress.port
    }

    @Betamax(tape="create-tape")
    def "Create command installs a template with sub-generators"() {
        given: "A target application directory"
        def appDir = new File(baseWorkDir, "maa")

        when: "I run lazybones with the create command for a template that has sub-templates"
        assert runCommand(
                ["create", "subtemplates-tmpl", "0.1", appDir.name, "-Pgroup=foo", "-Pversion=0.1"],
                appDir.parentFile) == 0
        assert appDir.exists()
        assert new File(appDir, "build.gradle").exists()


        and: "Use the generate command within the new project"
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

}
