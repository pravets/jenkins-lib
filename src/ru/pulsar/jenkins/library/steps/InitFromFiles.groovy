package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner

class InitFromFiles implements Serializable {

    private final JobConfiguration config;

    InitFromFiles(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.infoBaseFromFiles()) {
            Logger.println("init infoBase from files is disabled")
            return
        }

        steps.installLocalDependencies();

        Logger.println("Распаковка файлов")

        String srcDir;
        String srcExtDir;
        String srcExtBuildDir;
        String[] extNames;

        def env = steps.env();

        if (config.sourceFormat == SourceFormat.EDT) {
          
            srcDir = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.CONFIGURATION_DIR"

            steps.unstash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH)
            steps.unzip(srcDir, EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)
            
            srcExtDir = config.srcExtDir
            extNames = config.extNames

            if (srcExtDir.size() > 0 && extNames.size() > 0) {
                srcExtBuildDir = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.EXT_DIR"
                extNames.each {
                    steps.unstash("ext-${it}-zip")
                    steps.unzip("$srcExtBuildDir/${it}", "build/ext-${it}.zip")
                }
            }

        } else {
            srcDir = config.srcDir;
        }

        Logger.println("Выполнение загрузки конфигурации из файлов")
        String vrunnerPath = VRunner.getVRunnerPath();
        def initCommand = "$vrunnerPath init-dev --src $srcDir --ibconnection \"/F./build/ib\""
        VRunner.exec(initCommand)

        if (srcExtDir.size() > 0 && extNames.size() > 0) {
                extNames.each {
                    srcExtBuildDir = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.EXT_DIR"
                    Logger.println("Выполнение загрузки конфигурации расширения \"${it}\" из файлов")
                    def initExtCommand = "$vrunnerPath compileext $srcExtBuildDir/${it} ${it} --ibconnection \"/F./build/ib\" --updatedb"
                    VRunner.exec(initExtCommand)
                }
        }

    }
}
