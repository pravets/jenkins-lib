package ru.pulsar.jenkins.library.steps


import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Constants
import ru.pulsar.jenkins.library.utils.EDT
import ru.pulsar.jenkins.library.utils.Logger

class EdtToDesignerFormatTransformation implements Serializable {

    public static final String WORKSPACE = 'build/edt-workspace'
    public static final String WORKSPACE_EXT = 'build/edt-ext-workspace'
    public static final String CONFIGURATION_DIR = 'build/cfg'
    public static final String EXT_DIR = 'build/ext'
    public static final String CONFIGURATION_ZIP = 'build/cfg.zip'
    public static final String CONFIGURATION_ZIP_STASH = 'cfg-zip'

    private final JobConfiguration config;

    EdtToDesignerFormatTransformation(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (config.sourceFormat != SourceFormat.EDT) {
            Logger.println("SRC is not in EDT format. No transform is needed.")
            return
        }

        def env = steps.env();

        def srcDir = config.srcDir
        def srcExtDirs = config.srcExtDirs
        def projectDir = new File("$env.WORKSPACE/$srcDir").getCanonicalPath()
        def workspaceDir = "$env.WORKSPACE/$WORKSPACE" 
        def configurationRoot = "$env.WORKSPACE/$CONFIGURATION_DIR"
        def edtVersionForRing = EDT.ringModule(config)

        steps.deleteDir(workspaceDir)
        steps.deleteDir(configurationRoot)

        Logger.println("Конвертация исходников из формата EDT в формат Конфигуратора")

        def ringCommand = "ring $edtVersionForRing workspace export --workspace-location \"$workspaceDir\" --project \"$projectDir\" --configuration-files \"$configurationRoot\""

        def ringOpts = [Constants.DEFAULT_RING_OPTS]
        steps.withEnv(ringOpts) {
        //    steps.cmd(ringCommand)
        }

        //steps.zip(CONFIGURATION_DIR, CONFIGURATION_ZIP)
        //steps.stash(CONFIGURATION_ZIP_STASH, CONFIGURATION_ZIP)

        //String workspaceExtDir
        //String projectExtDir

        srcExtDirs.each{
            Logger.println("Путь к расширению ${it}")
            def extPathParts = it.split('/')
            def extName = extPathParts[extPathParts.size() - 2]

            def projectExtDir = new File("$env.WORKSPACE/${it}").getCanonicalPath()
            def workspaceExtDir = "$env.WORKSPACE/${extName}" 
         
            def configurationExtRoot = "$env.WORKSPACE/$WORKSPACE_EXT/$EXT_DIR/${extName}"
            
            def configurationExtZip = "build/ext-${extName}.zip"
            def configurationExtZipStash = "${extName}-zip"

            def ringCommandExt = "ring $edtVersionForRing workspace export --workspace-location \"$workspaceExtDir\" --project \"$projectExtDir\" --configuration-files \"$configurationExtRoot\""

            steps.deleteDir(workspaceExtDir)
            steps.deleteDir(configurationExtRoot)

            Logger.println("Конвертация исходников расширения ${it} из формата EDT в формат Конфигуратора")                

            steps.withEnv(ringOpts) {
                steps.cmd(ringCommandExt)
            }

            steps.zip(configurationExtRoot, configurationExtZip)
            steps.stash("$extSuffix${it}_$CONFIGURATION_ZIP_STASH", configurationExtZip)

        }
    }

}
