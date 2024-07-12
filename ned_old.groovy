pipeline{
    agent {
    label {
        label 'Kartofel'
        customWorkspace "D:/Jenkins/Workspace/NED/"
        }
    }
    environment {
       def filePath = "D:/Jenkins/Workspace/NED/NeverEndingDungeon/Config/DefaultGame.ini"
       def filePath2 = "D:/Jenkins/Workspace/NED/NeverEndingDungeon/Config/DefaultEngine.ini"
    
       def originalIni = readFile(filePath2)
    }
    stages {
  stage('Checkout') {
   steps{
   script{
      if (buid_type == 'Latest') {
        echo "Latest build"
         if (checkout_clean =='Yes'){
        p4sync charset: 'none', credential: 'NED_runner', format: 'jenkins-NED-Node1', populate: autoClean(delete: false, modtime: false, parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'], pin: '', quiet: true, replace: true, tidy: false), source: streamSource("$p4stream")
        } else if (checkout_clean =='No'){
        p4sync charset: 'none', credential: 'NED_runner', format: 'jenkins-NED-Node1', populate: syncOnly(force: false, have: true, modtime: false, parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'], pin: '', quiet: true, revert: false), source: streamSource("$p4stream")
        }//Do odkomentowania w razie W
        //p4sync charset: 'none', credential: 'NED_runner', format: 'jenkins-NED-Node1', populate: autoClean(delete: true, modtime: false, parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'], pin: '', quiet: true, replace: true, tidy: false), source: streamSource("$p4stream")
      } else if (buid_type == 'From commit') {
        echo "Build from specific $commit_id commit"
         if (checkout_clean =='Yes'){
        p4sync charset: 'none', credential: 'NED_runner', format: 'jenkins-NED-Node1', populate: autoClean(delete: false, modtime: false, parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'], pin: '$commit_id', quiet: true, replace: true, tidy: false), source: streamSource("$p4stream")
        }else if (checkout_clean =='No'){
        p4sync charset: 'none', credential: 'NED_runner', format: 'jenkins-NED-Node1', populate: syncOnly(force: false, have: true, modtime: false, parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'], pin: '$commit_id', quiet: true, revert: false), source: streamSource("$p4stream")
        }//p4sync charset: 'none', credential: 'NED_runner', format: 'jenkins-NED', populate: forceClean(have: true, parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'], pin: '$commit_id', quiet: true), source: streamSource("$p4stream")
      }
    
    def SUFFIX = '_NED' // Stały sufiks
    bat "attrib -r ${filePath}"
    bat "attrib -r ${filePath2}"
    def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}"
    /*def steam_config=''
    def epic_config=''
    def config_to_read=''
    def config_file= new File(filePath)
    //Podmienianie configu na Steam\Epic*/
    /*if (store == 'EGS') {
        def newContent2 = readFile(filePath2).replaceAll('DefaultPlatformService=EOSPlus', ";DefaultPlatformService=EOSPlus")
        writeFile(file: filePath2, text: newContent2)
        def newContent3 = readFile(filePath2).replaceAll('NativePlatformService=Steam', ";NativePlatformService=Steam")
        writeFile(file: filePath2, text: newContent3)
    }
    else */
    if (store == 'Steam') {
        echo "Steam config"
        
        def newContent2 = readFile(filePath2).replaceAll(';', "")
        writeFile(file: filePath2, text: newContent2)
        def newContent3 = readFile(filePath2).replaceAll('DefaultPlatformService=EOS', "DefaultPlatformService=EOSPlus")
        writeFile(file: filePath2, text: newContent3)
    }
    
                    def newContent = readFile(filePath).replaceAll('ProjectVersion=.+', "ProjectVersion=${COMBINED_VERSION}")
                          
                    writeFile(file: filePath, text: newContent)      
}
   }
  }

  stage('Build') {
    steps {
      script{
          def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))
         def folder_name = "D:/Builds/NED/${P4_CHANGELIST}_NED_${conf_short}"
         
         def storeName = "UNKNOWN"
         if(store == 'Steam')
         {
          
          echo"Nazwa folderu $folder_name"
         if (checkout_clean =='Yes'){
        bat """
"C:/Program Files/Epic Games/UE_5.3/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -clean -pak -compile -CloudDir="D:/Jenkins/Workspace/NED/Cloud" -compileeditor -installed -project="D:/Jenkins/Workspace/NED/NeverEndingDungeon/NeverEndingDungeon.uproject" -nop4 -utf8output  -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig=%build_conf% -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -ubtargs=-steam -log="D:/Jenkins/Workspace/NED/Logs/NED_Log.txt"
"""     
         }else if (checkout_clean =='No'){
         bat """
"C:/Program Files/Epic Games/UE_5.3/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -pak -compile -CloudDir="D:/Jenkins/Workspace/NED/Cloud" -compileeditor -installed -project="D:/Jenkins/Workspace/NED/NeverEndingDungeon/NeverEndingDungeon.uproject" -nop4 -utf8output  -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig=%build_conf% -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -ubtargs=-steam -log="D:/Jenkins/Workspace/NED/Logs/NED_Log.txt"
"""  
         }}
         else if(store == 'EGS')
         {
            
     echo"Nazwa folderu $folder_name"
         if (checkout_clean =='Yes'){
        bat """
"C:/Program Files/Epic Games/UE_5.3/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -clean -pak -compile -CloudDir="D:/Jenkins/Workspace/NED/Cloud" -compileeditor -installed -project="D:/Jenkins/Workspace/NED/NeverEndingDungeon/NeverEndingDungeon.uproject" -nop4 -utf8output  -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig=%build_conf% -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -log="D:/Jenkins/Workspace/NED/Logs/NED_Log.txt"
"""     
         }else if (checkout_clean =='No'){
         bat """
"C:/Program Files/Epic Games/UE_5.3/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -pak -compile -CloudDir="D:/Jenkins/Workspace/NED/Cloud" -compileeditor -installed -project="D:/Jenkins/Workspace/NED/NeverEndingDungeon/NeverEndingDungeon.uproject" -nop4 -utf8output  -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig=%build_conf% -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -log="D:/Jenkins/Workspace/NED/Logs/NED_Log.txt"
"""       
         }
}
      }
      //bat '"D:/Program Files/Epic Games/UE_5.3/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -compile -compileeditor -installed -nop4 -project="D:/Jenkins/Workspace/NED/NeverEndingDungeon/NeverEndingDungeon.uproject" -cook -stage -archive -archivedirectory="D:/Jenkins/Workspace/NED/NeverEndingDungeon/Development/x64" -package -clientconfig=Development -unrealexe=UnrealEditor-Cmd.exe -pak -prereqs -distribution -nodebuginfo -targetplatform=Win64 -build -AttachRenderDoc -utf8output -stagingdirectory="D:/Builds/NED/%P4_CHANGELIST%_NED/" -log="D:/Jenkins/Workspace/NED/Logs/NED_Log.txt"'
    }
  }

  stage('Zipping') {
    steps {
        script{
            def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))
        
        def build_path = "D:/Builds/NED/${P4_CHANGELIST}_NED_${conf_short}/Windows/"
        def compressed_path = "D:/Builds_Compressed/NED/${P4_CHANGELIST}_NED_${conf_short}.zip"

        bat """
        "C:\\Program Files\\7-Zip\\7z.exe" a -mx0 "${compressed_path}" "${build_path}"
        """
        }
    }
  }
  
  stage('EGS Upload') {
    steps {
        script{
            if (store == 'EGS' || store == 'Both') {
            def latest = readFile 'D:/Jenkins/Workspace/NED_latest.txt'
                    echo "Value from file: ${latest}"
            if (buid_type == 'Latest' && latest<P4_CHANGELIST) {
        echo "Latest build"
    def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))   
    def SUFFIX = '_NED' // StaĹ‚y sufiks
    
    def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}_${conf_short}"
    def build_path = "D:/Builds/NED/${COMBINED_VERSION}/Windows/"

bat"""
 "D:/Build Patch Tool/Engine/Binaries/Win64/BuildPatchTool.exe" -OrganizationId="o-tjbpeg9z2wuydsmqfuf3bjqtn59g7w" -ProductId="44a316f76dd34389877a9983da0b996d" -ArtifactId="4e9dc82133b64c18bbd17d67f4a751e3" -ClientId="xyza7891aSr3VDAOdP6L6TGFhawNAVJm" -ClientSecret="4RbGuuMrG3PSIXtZf4OrSBRe27DzFCfkKMw6qT4Rv2w" -mode=UploadBinary -BuildRoot="${build_path}" -CloudDir="D:/Jenkins/Workspace/NED_Cloud" -BuildVersion="${COMBINED_VERSION}" -AppLaunch="NeverEndingDungeon.exe" -Platform="Windows" -AppArgs="" -FileAttributeList="" -FileIgnoreList=""
 """
    def value = "${P4_CHANGELIST}"
    writeFile file: 'D:/Jenkins/Workspace/NED_latest.txt', text: value
    //bat '"D:/Build Patch Tool/Engine/Binaries/Win64/BuildPatchTool.exe" -OrganizationId="o-tjbpeg9z2wuydsmqfuf3bjqtn59g7w" -ProductId="44a316f76dd34389877a9983da0b996d" -ArtifactId="4e9dc82133b64c18bbd17d67f4a751e3" -ClientId="xyza7891aSr3VDAOdP6L6TGFhawNAVJm" -ClientSecret="4RbGuuMrG3PSIXtZf4OrSBRe27DzFCfkKMw6qT4Rv2w" -mode=LabelBinary -BuildVersion="%COMBINED_VERSION%" -AppLaunch="NeverEndingDungeon.exe" -Platform="Windows, Win32" -Label="Live" -AppArgs="" -FileAttributeList="" -FileIgnoreList=""'
      }
            
        }
        else if (store == 'Steam') {
              echo "Skipping Upload!"
          }
            
        }
      
    }}
  
  
  stage('Steam Upload') {
      steps{
          script{
              if (store == 'Steam' || store == 'Both') {
                  if (buid_type == 'Latest') {
                def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))
        echo "Latest build"
       def app_1 = "D:/Jenkins/steam/sdk/NED/tools/ContentBuilder/scripts/app_1896060.vdf"
       def app_2 = "D:/Jenkins/steam/sdk/NED/tools/ContentBuilder/scripts/depot_1896061.vdf"
    def SUFFIX = '_NED' // StaĹ‚y sufiks
    
    def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}_${conf_short}"
    
                    def branch =  readFile(app_1).replaceAll(/.*"setlive".*/, "\"setlive\" \"${steam_branch}\"")
                    writeFile(file: app_1, text: branch)
                    def newContent =  readFile(app_1).replaceAll(/.*"desc".*/, "\"desc\" \"${COMBINED_VERSION}\"")
                    writeFile(file: app_1, text: newContent)   
    def fileContent2 = readFile(app_2)
    
                    def newContent2 = readFile(app_2).replaceAll(/.*"contentroot".*/, "\"contentroot\" \"D:\\\\Builds\\\\NED\\\\${COMBINED_VERSION}\\\\Windows\\\"")

                    //def newContent2 = readFile(app_2).replaceAll(/.*"contentroot".*/, "\"contentroot\" \"D:\\Builds\\SHS\\${COMBINED_VERSION}\\Windows\\\"")

                    writeFile(file: app_2, text: newContent2)   
                    
            
                    withCredentials([usernamePassword(credentialsId: 'buildmachine_password', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        bat """D:/Jenkins/steam/sdk/NED/tools/ContentBuilder/builder/steamcmd.exe +login ${USERNAME} "${PASSWORD}" +run_app_build "D:/Jenkins/steam/sdk/NED/tools/ContentBuilder/scripts/app_1896060.vdf" +quit"""
                    }
            //bat 'D:/Jenkins/steam/sdk/SHS/tools/ContentBuilder/builder/steamcmd.exe  +login BBG_buildmachine 'buildmachine_password' +run_app_build "D:/Jenkins/steam/sdk/SHS/tools/ContentBuilder/scripts/app_1442430.vdf" +quit"'
      }
              }
              
              else if (store == 'EGS') {
              echo "Skipping Upload!"
          }
      }
  }
  

}}
post {
 always{
     script{
     logParser failBuildOnError: false, parsingRulesPath: 'D:\\Jenkins\\parser.txt', projectRulePath: 'D:/Jenkins/parser.txt', useProjectRule: false    
         dir("C:/Users/Master/AppData/Roaming/Unreal Engine/AutomationTool/Logs/C+Program+Files+Epic+Games+UE_5.3") {
    archiveArtifacts '*.txt'
     }
        bat 'del /q /s "C:\\Users\\Master\\AppData\\Roaming\\Unreal Engine\\AutomationTool\\Logs\\C+Program+Files+Epic+Games+UE_5.3\\*"'
        
        //Revertowanie pliku ini
        writeFile(file: filePath2, text: originalIni)
        
        
        //bat "move /Y ${filePath3} ${filePath2}"
        //def newContent2 = readFile(filePath2).replaceAll('DefaultPlatformService=EOSPlus', ";DefaultPlatformService=EOSPlus")
        //writeFile(file: filePath2, text: newContent2)
        //def newContent3 = readFile(filePath2).replaceAll('NativePlatformService=Steam', ";NativePlatformService=Steam")
        //writeFile(file: filePath2, text: newContent3)
     }
  }
  success {
    script{      
    def p4Command = "p4 -u qa -p ssl:34.118.124.6:1666 describe -s ${P4_CHANGELIST}"
                    
    // Wykonaj polecenie i przechwyć wynik
    def p4Result = bat(script: "cmd /c ${p4Command}",returnStdout: true).trim()

    echo "Wynik komendy p4: ${p4Result}"

    // Filtrowanie linii
    def filteredText = p4Result.replaceAll(/(Change.*|Affected.*|\.\.\..*|D:.*)\n?/, '')

    // Usunięcie pustych linii
    def nonEmptyLines = filteredText.readLines().findAll { it.trim() != '' }

    // Łączenie przefiltrowanych linii w tekst
    def finalFilteredText = nonEmptyLines.join('\n')
    finalFilteredText = finalFilteredText.replace("#changelist validated", "").trim()
    def SUFFIX = '_NED' // Stały sufiks
    def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))
    def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}"
    currentBuild.description = "BUILD ID: ${COMBINED_VERSION}_${conf_short} ${store}" 
    slackSend botUser: true, channel: '#ned-ci-cd', color: '#00FF00', message: "<@U051VA48N0P><@U05KPADLNRY> New build available: $P4_CHANGELIST NED Commit description:\n*${finalFilteredText}* <https://drive.google.com/drive/folders/1-b8JPLS7CUS_-pACPXX0tKKbNm75U8Hj|NED folder>", tokenCredentialId: 'NED_Bot'
    }
  }
  failure {
     
   script {
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
                def filePath = "//GITRUNNER2/jobs//${JOB_NAME}/builds/${BUILD_NUMBER}/logerrorLinks.html"
                 
                    def content = new File(filePath).text
               
                
                    def pattern = /<li>.*?red">/
                    def pattern2 = "</span></a></li>"
                    
                    
                    def cleanedContent = content.replaceAll(pattern, '')
                    cleanedContent = cleanedContent.replaceAll(pattern2,'')
                    
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
