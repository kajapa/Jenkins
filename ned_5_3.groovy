//parameters
def REPOSITORY_DIR_PATH = "D:\\Jenkins\\Workspace\\NED_5_3"
def GAME_CONFIG_INI_PATH = "D:/Jenkins/Workspace/NED_5_3/NeverEndingDungeon/Config/DefaultGame.ini"
def ENGINE_CONFIG_INI_PATH = "D:/Jenkins/Workspace/NED_5_3/NeverEndingDungeon/Config/DefaultEngine.ini"
def COMMIT_FILE = "D:/Jenkins/Workspace/NED_latest.txt"
def AGENT_LABEL = 'Kartofel'
def PROJECT_SUFFIX = "_NED"
def SKIP_BUILD = "false"
def CONF_SHORT = env.build_conf.substring(0, Math.min(env.build_conf.length(), 5)) 
def APP_FILE = "D:/Jenkins/steam/sdk/NED/tools/ContentBuilder/scripts/app_1896060.vdf"
def DEPOT_FILE = "D:/Jenkins/steam/sdk/NED/tools/ContentBuilder/scripts/depot_1896061.vdf"

//vars
def CHANGESET_PIN = ''
def BUILD_ARGS = ""
def FINAL_ARGS = ""
def GAME_CONFIG_STREAM_PATH = ""
def ENGINE_CONFIG_STREAM_PATH = ""

//methods
def SyncP4WorkspaceWithClean(InStream, InPin)
{
  p4sync charset: 'none', 
  credential: 'NED_runner', 
  format: 'jenkins-NED-UE_5_3', 
  populate: autoClean(
    delete: false, 
    modtime: true, 
    parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'], 
    pin: "$InPin", 
    quiet: true, 
    replace: true, 
    tidy: false), 
  source: streamSource("$InStream")
}

def SyncP4WorkspaceWithoutClean(InStream, InPin)
{
  p4sync charset: 'none', 
  credential: 'NED_runner', 
  format: 'jenkins-NED-UE_5_3', 
  populate: syncOnly(
    force: false, 
    have: true, 
    modtime: false, 
    parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'], 
    pin: "$InPin", 
    quiet: true, 
    revert: false), 
  source: streamSource("$InStream")
}

def TriggerBuild(InClean, InArgs, InFolderName, InBuildConf)
{
  if(InClean == 'Yes')
  {
    bat """
"C:/Program Files/Epic Games/UE_5.3/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -clean -pak -compile -CloudDir="D:/Jenkins/Workspace/NED_5_3/Cloud" -compileeditor -installed -project="D:/Jenkins/Workspace/NED_5_3/NeverEndingDungeon/NeverEndingDungeon.uproject" -nop4 -utf8output  -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig="${InBuildConf}" -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${InFolderName}" -ubtargs="${InArgs}" -log="D:/Jenkins/Workspace/NED_5_3/Logs/NED_Log.txt"
"""  
  }
  else if(InClean == 'No')
  {
    bat """
"C:/Program Files/Epic Games/UE_5.3/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -pak -compile -CloudDir="D:/Jenkins/Workspace/NED_5_3/Cloud" -compileeditor -installed -project="D:/Jenkins/Workspace/NED_5_3/NeverEndingDungeon/NeverEndingDungeon.uproject" -nop4 -utf8output  -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig="${InBuildConf}" -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${InFolderName}" -ubtargs="${InArgs}" -log="D:/Jenkins/Workspace/NED_5_3/Logs/NED_Log.txt"
"""  
  }   
}

def TriggerEGSUpload(InBuildPath, InCombinedVersion)
{
  bat"""
"D:/Build Patch Tool/Engine/Binaries/Win64/BuildPatchTool.exe" -OrganizationId="o-tjbpeg9z2wuydsmqfuf3bjqtn59g7w" -ProductId="44a316f76dd34389877a9983da0b996d" -ArtifactId="4e9dc82133b64c18bbd17d67f4a751e3" -ClientId="xyza7891aSr3VDAOdP6L6TGFhawNAVJm" -ClientSecret="4RbGuuMrG3PSIXtZf4OrSBRe27DzFCfkKMw6qT4Rv2w" -mode=UploadBinary -BuildRoot="${InBuildPath}" -CloudDir="D:/Jenkins/Workspace/NED_5_3_Cloud" -BuildVersion="${InCombinedVersion}" -AppLaunch="NeverEndingDungeon.exe" -Platform="Windows" -AppArgs="" -FileAttributeList="" -FileIgnoreList=""
"""
}

def TriggerXsollaUpload(InBuildPath, InCombinedVersion)
{
  bat"""
"D:/Jenkins/Xsolla/Win/build_loader.exe" --update --set-build-on-test  --game-path "${InBuildPath}" --descr "${InCombinedVersion}"
"""
}


pipeline
{
  agent
  {
    label
    {
      label AGENT_LABEL
      customWorkspace REPOSITORY_DIR_PATH
    } 
  }

  stages
  {
    stage('Prepare: Pipeline Configuration')
    {
      steps
      {
        script
        {
          GAME_CONFIG_STREAM_PATH = "${env.p4stream}/NeverEndingDungeon/Config/DefaultGame.ini"
          ENGINE_CONFIG_STREAM_PATH = "${env.p4stream}/NeverEndingDungeon/Config/DefaultEngine.ini"

          if(env.build_type == 'From commit')
          {
            echo "From Commit: $env.commit_id"
            CHANGESET_PIN = "$env.commit_id"
          }

          if(env.maps_to_cook == 'reduced')
          {
            echo "Maps to cook: $env.maps_to_cook"
            BUILD_ARGS = "-nomap"
          }
        }
      }
    }
    
    stage('Checkout: P4V')
    {
      steps
      {
        script
        {
          if(env.checkout_clean == 'Yes')
          {
            SyncP4WorkspaceWithClean(env.p4stream, CHANGESET_PIN)
          }
          else if(env.checkout_clean == 'No')
          {
            SyncP4WorkspaceWithoutClean(env.p4stream, CHANGESET_PIN)
          }
          else
          {
            echo "Unrecognized checkout policy: ${env.checkout_clean}. Performing sync without cleaning."
            SyncP4WorkspaceWithoutClean(env.p4stream, CHANGESET_PIN)
          }

          currentBuild.description = "BUILD ID: ${P4_CHANGELIST} ${env.build_conf} ${env.store}" 
        }
      }
    }

    stage('Check for changes') 
    {
      steps {
        script 
        {
          def triggered_by_user = currentBuild.getBuildCauses()[0].userId
          echo "Build został wywołany przez użytkownika: ${triggered_by_user}."
          def p4commit = "${P4_CHANGELIST}".toInteger()
        
          def read_commit = readFile(COMMIT_FILE).trim().toInteger()
          if(triggered_by_user == null && p4commit == read_commit)
          {
            echo('Skipping building from the same commit')
            SKIP_BUILD = "true"
            currentBuild.description ="SKIPPED"
            currentBuild.result = 'ABORTED'
          }
            writeFile(file: COMMIT_FILE, text: p4commit.toString())
          
        }
      }
    }

    stage('Prepare: Steam')
    {
      when
      {
        expression
        {
          return (env.store == 'Steam' || env.store == 'Both') && SKIP_BUILD == 'false'
        }
      }

      steps
      {
        script
        {
          def gameConfigCheckoutCommand = "p4 -u qa -p ssl:34.118.124.6:1666 -c jenkins-NED-UE_5_3 edit -c default ${GAME_CONFIG_INI_PATH}"
          def engineConfigCheckoutCommand = "p4 -u qa -p ssl:34.118.124.6:1666 -c jenkins-NED-UE_5_3 edit -c default ${ENGINE_CONFIG_INI_PATH}"  

          bat(script: "cmd /c ${gameConfigCheckoutCommand}")
          bat(script: "cmd /c ${engineConfigCheckoutCommand}")

          def steamEngineIni = readFile(ENGINE_CONFIG_INI_PATH).replaceAll(';', "")
          writeFile(file: ENGINE_CONFIG_INI_PATH, text: steamEngineIni)
            
          def eosPlusEngineIni = readFile(ENGINE_CONFIG_INI_PATH).replaceAll('DefaultPlatformService=EOS', "DefaultPlatformService=EOSPlus")
          writeFile(file: ENGINE_CONFIG_INI_PATH, text: eosPlusEngineIni)

          def COMBINED_VERSION = "${P4_CHANGELIST}${PROJECT_SUFFIX}"

          if(env.maps_to_cook == 'reduced')
          {
            def reducedGameIni = readFile(GAME_CONFIG_INI_PATH).replaceAll(';', "")
            writeFile(file: GAME_CONFIG_INI_PATH, text: reducedGameIni)
          }
        
          def updatedProjectVersion = readFile(GAME_CONFIG_INI_PATH).replaceAll('ProjectVersion=.+', "ProjectVersion=${COMBINED_VERSION}")
          writeFile(file: GAME_CONFIG_INI_PATH, text: updatedProjectVersion)

          FINAL_ARGS = BUILD_ARGS + " -steam"
        }
      }
    }

    stage('Build: Steam')
    {
      when
      {
        expression
        {
          return (env.store == 'Steam' || env.store == 'Both') && SKIP_BUILD == 'false'
        }
      }

      steps
      {
        script
        {
          
          def folder_name = "D:/Builds/NED/${P4_CHANGELIST}_NED_${CONF_SHORT}"

          echo "Nazwa folderu $folder_name"
          echo "Build Args $FINAL_ARGS"

          TriggerBuild(env.checkout_clean, FINAL_ARGS, folder_name, env.build_conf)

          def gameConfigCheckoutCommand = "p4 -u qa -p ssl:34.118.124.6:1666 -c jenkins-NED-UE_5_3 revert ${GAME_CONFIG_STREAM_PATH}"
          def engineConfigCheckoutCommand = "p4 -u qa -p ssl:34.118.124.6:1666 -c jenkins-NED-UE_5_3 revert ${ENGINE_CONFIG_STREAM_PATH}"  

          bat(script: "cmd /c ${gameConfigCheckoutCommand}")
          bat(script: "cmd /c ${engineConfigCheckoutCommand}")
        }
      }
    }

    stage('Upload: Steam')
    {
      when
      {
        expression
        {
          return (env.store == 'Steam' || env.store == 'Both') && SKIP_BUILD == 'false'
        }
      }

      steps
      {
        script
        {
          
          echo "Latest build"
          
          def COMBINED_VERSION = "${P4_CHANGELIST}${PROJECT_SUFFIX}_${CONF_SHORT}"
          def branch =  readFile(APP_FILE).replaceAll(/.*"setlive".*/, "\"setlive\" \"${env.steam_branch}\"")
          writeFile(file: APP_FILE, text: branch)
          def newContent =  readFile(APP_FILE).replaceAll(/.*"desc".*/, "\"desc\" \"${COMBINED_VERSION}\"")
          writeFile(file: APP_FILE, text: newContent)   
          
          def newContent2 = readFile(DEPOT_FILE).replaceAll(/.*"contentroot".*/, "\"contentroot\" \"D:\\\\Builds\\\\NED\\\\${COMBINED_VERSION}\\\\Windows\\\"")

          writeFile(file: DEPOT_FILE, text: newContent2)   
          withCredentials([usernamePassword(credentialsId: 'buildmachine_password', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) 
          {
            bat """D:/Jenkins/steam/sdk/NED/tools/ContentBuilder/builder/steamcmd.exe +login ${USERNAME} "${PASSWORD}" +run_app_build "D:/Jenkins/steam/sdk/NED/tools/ContentBuilder/scripts/app_1896060.vdf" +quit"""
          }
        }
      }
    }

    stage('Prepare: EGS')
    {
      when
      {
        expression
        {
          return (env.store == 'EGS' || env.store == 'Both' || env.store == 'Xsolla') && SKIP_BUILD == 'false'
        }
      }

      steps
      {
        script
        {
          def gameConfigCheckoutCommand = "p4 -u qa -p ssl:34.118.124.6:1666 -c jenkins-NED-UE_5_3 edit -c default ${GAME_CONFIG_INI_PATH}"
          def engineConfigCheckoutCommand = "p4 -u qa -p ssl:34.118.124.6:1666 -c jenkins-NED-UE_5_3 edit -c default ${ENGINE_CONFIG_INI_PATH}"  

          bat(script: "cmd /c ${gameConfigCheckoutCommand}")
          bat(script: "cmd /c ${engineConfigCheckoutCommand}")

          def COMBINED_VERSION = "${P4_CHANGELIST}${PROJECT_SUFFIX}"

          if(env.maps_to_cook == 'reduced')
          {
            def reducedGameIni = readFile(GAME_CONFIG_INI_PATH).replaceAll(';', "")
            writeFile(file: GAME_CONFIG_INI_PATH, text: reducedGameIni)
          }
        
          def updatedProjectVersion = readFile(GAME_CONFIG_INI_PATH).replaceAll('ProjectVersion=.+', "ProjectVersion=${COMBINED_VERSION}")
          writeFile(file: GAME_CONFIG_INI_PATH, text: updatedProjectVersion)

          FINAL_ARGS = BUILD_ARGS
        }
      }
    }

    stage('Build: EGS')
    {
      when
      {
        expression
        {
          return (env.store == 'EGS' || env.store == 'Both' || env.store == 'Xsolla') && SKIP_BUILD == 'false'
        }
      }

      steps
      {
        script
        {
          
          def folder_name = "D:/Builds/NED/${P4_CHANGELIST}_NED_${CONF_SHORT}"

          echo "Nazwa folderu $folder_name"
          echo "Build Args $FINAL_ARGS"

          TriggerBuild(env.checkout_clean, FINAL_ARGS, folder_name, env.build_conf)

          def gameConfigCheckoutCommand = "p4 -u qa -p ssl:34.118.124.6:1666 -c jenkins-NED-UE_5_3 revert ${GAME_CONFIG_STREAM_PATH}"
          def engineConfigCheckoutCommand = "p4 -u qa -p ssl:34.118.124.6:1666 -c jenkins-NED-UE_5_3 revert ${ENGINE_CONFIG_STREAM_PATH}"  

          bat(script: "cmd /c ${gameConfigCheckoutCommand}")
          bat(script: "cmd /c ${engineConfigCheckoutCommand}")
        }
      }
    }

    stage('Upload: EGS')
    {
      when
      {
        expression
        {
          return (env.store == 'EGS' || env.store == 'Both') && SKIP_BUILD == 'false'
        }
      }

      steps
      {
        script
        {
          def latest = readFile 'D:/Jenkins/Workspace/NED_latest.txt'
          echo "Value from file: ${latest}"

          if (env.buid_type == 'Latest' && latest!=P4_CHANGELIST)
          {
            echo "Latest build"
            

            def COMBINED_VERSION = "${P4_CHANGELIST}${PROJECT_SUFFIX}_${CONF_SHORT}"
            def build_path = "D:/Builds/NED/${COMBINED_VERSION}/Windows/"

            TriggerEGSUpload(build_path, COMBINED_VERSION)

            def value = "${P4_CHANGELIST}"
            writeFile file: 'D:/Jenkins/Workspace/NED_latest.txt', text: value         
          }
        }
      }
    }
    stage('Upload: Xsolla')
    {
      when
      {
        expression
        {
          return (env.store == 'Xsolla') && SKIP_BUILD == 'false'
        }
      }

      steps
      {
        script
        {

          def COMBINED_VERSION = "${P4_CHANGELIST}${PROJECT_SUFFIX}_${CONF_SHORT}"
          def build_path = "D:/Builds/NED/${COMBINED_VERSION}/Windows/"
          TriggerXsollaUpload(build_path, COMBINED_VERSION)
                    
        }
      }
    }

    stage('Zipping')
    {
      when
      {
        expression
        {
          return SKIP_BUILD == 'false'
        }
      }
      steps
      {
        script
        {
          
        
          def build_path = "D:/Builds/NED/${P4_CHANGELIST}_NED_${CONF_SHORT}/Windows/"
          def compressed_path = "D:/Builds_Compressed/NED/${P4_CHANGELIST}_NED_${CONF_SHORT}.zip"

          bat """
            "C:\\Program Files\\7-Zip\\7z.exe" a -mx0 "${compressed_path}" "${build_path}"
            """
        }
      }
    }
  }

  post
  {
    always
    {
      script
      {
        if(SKIP_BUILD == "false")
        {
        logParser failBuildOnError: false, 
        parsingRulesPath: 'D:\\Jenkins\\parser.txt', 
        projectRulePath: 'D:/Jenkins/parser.txt', 
        useProjectRule: false    
        dir("C:/Users/Master/AppData/Roaming/Unreal Engine/AutomationTool/Logs/C+Program+Files+Epic+Games+UE_5.3")
        {
          archiveArtifacts '*.txt'
        }

        bat 'del /q /s "C:\\Users\\Master\\AppData\\Roaming\\Unreal Engine\\AutomationTool\\Logs\\C+Program+Files+Epic+Games+UE_5.3\\*"'
        }
      }
    }
    success
    {
      script
      {
        def p4Command = "p4 -u qa -p ssl:34.118.124.6:1666 describe -s ${P4_CHANGELIST}"
        // Wykonaj polecenie i przechwyć wynik
        def p4Result = bat(script: "cmd /c ${p4Command}",returnStdout: true).trim()

        echo "Wynik komendy p4: ${p4Result}"
        def store_message = ""
        if (env.store == 'EGS') 
        {
          store_message="on Epic"
        }
        else if (env.store == 'Steam') 
        {
          store_message="on Steam on ${env.steam_branch} branch"
        }
        else if (env.store == 'Both') 
        {
          store_message="on Steam on ${env.steam_branch} branch and Epic"
        }
        else if (env.store == 'Xsolla')
        {
          store_message="on Xsolla"
        }


        // Filtrowanie linii
        def filteredText = p4Result.replaceAll(/(Change.*|Affected.*|\.\.\..*|D:.*)\n?/, '')

        // Usunięcie pustych linii
        def nonEmptyLines = filteredText.readLines().findAll { it.trim() != '' }

        // Łączenie przefiltrowanych linii w tekst
        def finalFilteredText = nonEmptyLines.join('\n')
        finalFilteredText = finalFilteredText.replace("#changelist validated", "").trim()
        
        def COMBINED_VERSION = "${P4_CHANGELIST}${PROJECT_SUFFIX}"
        currentBuild.description = "BUILD ID: ${COMBINED_VERSION}_${CONF_SHORT} ${env.store}" 
        slackSend botUser: true, channel: '#ned-ci-cd', color: '#00FF00', message: "<@U051VA48N0P><@U05KPADLNRY> New build available ${store_message}: $P4_CHANGELIST NED Commit description:\n*${finalFilteredText}* <https://drive.google.com/drive/folders/1x0APZIlKmipadNz4SI2YIdu4PSwNqlhe?usp=drive_link|NED folder>", tokenCredentialId: 'NED_Bot'
      }
    }
    failure
    {
      script
      {
        def p4Command = "p4 -u qa -p ssl:34.118.124.6:1666 describe -s ${P4_CHANGELIST}"
                  
        // Wykonaj polecenie i przechwyć wynik
        def p4Result = bat(script: "cmd /c ${p4Command}",returnStdout: true).trim()

        echo "Wynik komendy p4: ${p4Result}"

        // Filtrowanie linii
        def filteredText = p4Result.replaceAll(/(Change.*|Affected.*|\.\.\..*|D:.*)\n?/, '')

        // Usunięcie pustych linii
        def nonEmptyLines = filteredText.readLines().findAll { it.trim() != '' }

        // Łączenie przefiltrowanych linii w tekst
        def finalFilteredText = nonEmptyLines.join('\n').trim()
        finalFilteredText = finalFilteredText.replace("#changelist validated", "").trim()
            
        currentBuild.description = "COMMIT ID: ${P4_CHANGELIST}" 
        def gamefile = "//GITRUNNER2/jobs//${JOB_NAME}/builds/${BUILD_NUMBER}/logerrorLinks.html"
                
        def content = new File(gamefile).text
              
              
        def pattern = /<li>.*?red">/
        def pattern2 = "</span></a></li>"
                  
                  
        def cleanedContent = content.replaceAll(pattern, '')
        cleanedContent = cleanedContent.replaceAll(pattern2,'')
        cleanedContent = cleanedContent.replaceAll(/(LogWindows: Failed to load .*|Upgrade.*)\n?/, '')
        cleanedContent = cleanedContent.readLines().findAll { it.trim() != '' }
                  
        //Get submitter
                  
        def p4Command2 = "p4 -u qa -p ssl:34.118.124.6:1666 describe -s ${P4_CHANGELIST} | findstr @"
        def p4Result2 = bat(script: "cmd /c ${p4Command2}", returnStdout: true).trim()
        echo "Komenda bez filtracji ${p4Result2}"

        def submitter = p4Result2.replaceAll(/.*by | on .*/, "").replaceAll(/Workspace.*\n?/, "").replaceAll(/^.*D:.*\n?/, "").trim()
        echo "Wynik filtracji: ${submitter}"
              
        slackSend botUser: true, channel: '#ned-ci-cd', color: '#FF0000', message: "<@U04DQGUEPQT><@U051JHTD70C><@U05KPADLNRY> Build failed at commit: $P4_CHANGELIST Commit description:\n*${finalFilteredText}*\n submitted by *${submitter}* Link to <http://192.168.100.137:8080/job/${JOB_NAME}/${BUILD_NUMBER}/artifact/|artefacts> ${cleanedContent}", tokenCredentialId: 'NED_Bot'
      }
    }
  }
}