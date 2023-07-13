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
        def extNames = config.extNames

        if (config.sourceFormat == SourceFormat.EDT) {
            def env = steps.env();
            srcDir = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.CONFIGURATION_DIR"

            steps.unstash(EdtToDesignerFormatTransformation.CONFIGURATION_ZIP_STASH)
            steps.unzip(srcDir, EdtToDesignerFormatTransformation.CONFIGURATION_ZIP)

            if (config.needExtensions()) {

                extNames.each {extName ->
                    def Configuration_Ext_Dir = EdtExtensionsToDesignerFormatTransformation.pathToExtensionFiles(extName)
                    def Configuration_Ext_Zip = EdtExtensionsToDesignerFormatTransformation.pathToExtensionZip(extName)
                    def Configuration_Ext_Zip_Stash = EdtExtensionsToDesignerFormatTransformation.pathToExtensionZipStash(extName)
                    
                    steps.unstash(Configuration_Ext_Zip_Stash)
                    steps.unzip(Configuration_Ext_Dir, Configuration_Ext_Zip)
                }
            }
        } else {
            srcDir = config.srcDir;
        }

        Logger.println("Выполнение загрузки конфигурации из файлов")
        String vrunnerPath = VRunner.getVRunnerPath();
        def initCommand = "$vrunnerPath init-dev --src $srcDir --ibconnection \"/F./build/ib\""
        VRunner.exec(initCommand)

        if (config.needExtensions()) {
            if (config.sourceFormat == SourceFormat.EDT) {
                extNames.each {extName ->
                    Logger.println("Выполнение загрузки конфигурации расширения $extName из файлов")
                    def srcExtDir = EdtExtensionsToDesignerFormatTransformation.pathToExtensionFiles(extName)
                    def initExtCommand = "$vrunnerPath compileext $srcExtDir $extName --ibconnection \"/F./build/ib\" --updatedb"
                    VRunner.exec(initExtCommand)
                }
            } else {

            }
        }
    }
}
