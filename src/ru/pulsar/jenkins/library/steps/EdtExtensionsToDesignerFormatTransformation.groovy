package ru.pulsar.jenkins.library.steps


import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Constants
import ru.pulsar.jenkins.library.utils.EDT
import ru.pulsar.jenkins.library.utils.Logger

class EdtExtensionsToDesignerFormatTransformation implements Serializable {

    public static final String EDT_WORKSPACE = 'build/edt-workspace-ext'
    public static final String CONFIGURATION_DIR = 'build/cfe'
    public static final String CONFIGURATION_ZIP = 'build/cfe-EXT.zip'
    public static final String CONFIGURATION_ZIP_STASH = 'cfe-EXT-zip'

    private final JobConfiguration config;

    EdtExtensionsToDesignerFormatTransformation(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (config.sourceFormat != SourceFormat.EDT) {
            Logger.println("SRC is not in EDT format. No transform is needed.")
            return
        }

        def extNames = config.extNames

        def needCancelProcess = false

        if (extNames.size() == 0) {
            Logger.println("Не задан массив расширений для конвертации.")
            needCancelProcess = true
        }

        def srcExtDir = config.srcExtDir
        if (srcExtDir.length() == 0) {
            Logger.println("Не задан путь к каталогу расширений для конвертации.")
            needCancelProcess true
        }

        if (needCancelProcess) {
            Logger.println("Конвертация расширений из формата EDT в формат конфигуратора отменена.")
            return
        }

        def workspaceDir = "$env.WORKSPACE/$EDT_WORKSPACE"
        def configurationRoot = "$env.WORKSPACE/$CONFIGURATION_DIR"
        def edtVersionForRing = EDT.ringModule(config)
        def env = steps.env()

        extNames.each { extName ->

            def projectDir = new File("$env.WORKSPACE/$srcExtDir/$extName").getCanonicalPath()

            steps.deleteDir(workspaceDir)
            steps.deleteDir(configurationRoot)

            Logger.println("Конвертация исходников расширения $extName из формата EDT в формат Конфигуратора")

            def ringCommand = "ring $edtVersionForRing workspace export --workspace-location \"$workspaceDir\" --project \"$projectDir\" --configuration-files \"$configurationRoot\""

            def ringOpts = [Constants.DEFAULT_RING_OPTS]
            steps.withEnv(ringOpts) {
                steps.cmd(ringCommand)
            }
            def Configuration_Ext_Dir = CONFIGURATION_DIR.replace('EXT', extName)
            def Configuration_Ext_Zip = CONFIGURATION_ZIP.replace('EXT', extName)
            def Configuration_Ext_Zip_Stash = CONFIGURATION_ZIP_STASH.replace('EXT', extName)

            steps.zip(Configuration_Ext_Dir, Configuration_Ext_Zip)
            steps.stash(Configuration_Ext_Zip_Stash, Configuration_Ext_Zip)

        }
    }

}
