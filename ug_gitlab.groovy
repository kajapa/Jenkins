pipeline {
    agent {
    label {
        label 'Mechanic'
        customWorkspace 'D:/Jenkins/Workspace/UG_gitlab/'
    }
    }
    environment {
    def app_file = 'D:/Jenkins/steam/sdk/UG/tools/ContentBuilder/scripts/app_1452250.vdf'
    def depot_file = 'D:/Jenkins/steam/sdk/UG/tools/ContentBuilder/scripts/depot_1452251.vdf'
    def depot_file_with_pdb = 'D:/Jenkins/steam/sdk/UG/tools/ContentBuilder/scripts/depot_with_pdb.vdf'
    def depot_file_without_pdb = 'D:/Jenkins/steam/sdk/UG/tools/ContentBuilder/scripts/depot_without_pdb.vdf'
    def commit_file = 'D:/Jenkins/Workspace/UG_commit_id.txt'
    def gitCommand = '''cd /d D:/Jenkins/Workspace/UG_gitlab && git log --pretty="%%h" -n 1'''
    def skip_build = 'false'
    def commit_short = ''
    def SUFFIX = '_UG'
    }
    stages {
    stage('Checkout') {
      steps {
        script {
          def triggered_by_user = currentBuild.getBuildCauses()[0].userId
          echo "Build został wywołany przez użytkownika: ${triggered_by_user}."
            git branch: 'main', credentialsId: 'gitlab', url: 'https://gitlab.beardedbrothers.games/raccoons-studio/ug_migration.git'
          commit_short = bat(script: "cmd /c ${gitCommand}", returnStdout: true).trim()
          commit_short = commit_short.split('\n')[-1].trim()

          currentBuild.description = "BUILD ID: ${commit_short} ${build_conf}"

          // StaĹ‚y sufiks
          def depot_content = ''
          if (signfiles_upload == 'Yes') {
            depot_content = readFile(depot_file_with_pdb)
            writeFile(file: depot_file, text: depot_content)
       } else if (signfiles_upload == 'No') {
            depot_content = readFile(depot_file_without_pdb)
            writeFile(file: depot_file, text: depot_content)
          }
   }}
      }

    stage('Check for changes') {
      steps {
        script {
          def triggered_by_user = currentBuild.getBuildCauses()[0].userId

          def read_commit = readFile(commit_file).trim()
          if (triggered_by_user == null && commit_short.equals(read_commit)) {
            skip_build = 'true'
            currentBuild.description = 'SKIPPED'
            currentBuild.result = 'ABORTED'
          }
            writeFile(file: commit_file, text: commit_short)
   }}
      }

    stage('Build') {
      when
      {
        expression
        {
          return skip_build == 'false'
        }
      }
      steps {
        script {
          def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))

          def folder_name = "D:/Builds/UndergroundGarage/${commit_short}_UG_${conf_short}"
          echo"Nazwa folderu $folder_name"
          if (checkout_clean == 'Yes') {
            bat """
"D:/Program Files/Epic Games/UE_5.4/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -clean -pak -compile -compileeditor -installed -project="D:/Jenkins/Workspace/UG_gitlab/UndergroundGarage.uproject" -nop4 -utf8output  -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig="${env.build_conf}" -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -log="D:/Jenkins/Workspace/UG_gitlab/Logs/UG_Log.txt"
"""
         }else if (checkout_clean == 'No') {
            bat """
"D:/Program Files/Epic Games/UE_5.4/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -pak -compile -compileeditor -installed -project="D:/Jenkins/Workspace/UG_gitlab/UndergroundGarage.uproject" -nop4 -utf8output  -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig="${env.build_conf}" -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -log="D:/Jenkins/Workspace/UG_gitlab/Logs/UG_Log.txt"
"""
          }
    //bat '"D:/Program Files/Epic Games/UE_5.4/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -compile -compileeditor -installed -nop4 -project="D:/Jenkins/Workspace/UG_gitlab/UndergroundGarage.uproject" -cook -stage -archive -archivedirectory="D:/Jenkins/Workspace/UG/UndergroundGarage/UndergroundGarage/Development/x64" -package -clientconfig=%build_conf% -unrealexe=UnrealEditor-Cmd.exe -clean -pak -prereqs -distribution -nodebuginfo -targetplatform=Win64 -build -utf8output -stagingdirectory="D:/Builds/UndergroundGarage/%P4_CHANGELIST%_UG/" -log="D:/Jenkins/Workspace/UG/Logs/UG_Log.txt"'
    }}
      }

    stage('Zipping') {
      when
      {
        expression
        {
          return skip_build == 'false'
        }
      }
      steps {
        script {
          def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))

          def build_path = "D:/Builds/UndergroundGarage/${commit_short}_UG_${conf_short}/Windows/"
          def compressed_path = "D:/Builds_Compressed/UndergroundGarage/${commit_short}_UG_${conf_short}.zip"

          bat """
"C:\\Program Files\\7-Zip\\7z.exe" a -mx0 "${compressed_path}" "${build_path}"
"""
        }
      }
    }
    stage('Steam Upload') {
      when
      {
        expression
        {
          return skip_build == 'false'
        }
      }
      steps {
        script {
            if (buid_type == 'Latest') {
                def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))

            echo 'Latest build'

            def steam_branch1 = ''
            def COMBINED_VERSION = "${commit_short}${SUFFIX}_${conf_short}"

            def newContent =  readFile(env.app_file).replaceAll(/.*"desc".*/, "\"desc\" \"${COMBINED_VERSION}\"")
            writeFile(file: env.app_file, text: newContent)
            def fileContent2 = readFile(env.depot_file)

            def newContent2 = readFile(env.depot_file).replaceAll(/.*"contentroot".*/, "\"contentroot\" \"D:\\\\Builds\\\\UndergroundGarage\\\\${COMBINED_VERSION}\\\\Windows\\\"")

                    //def newContent2 = readFile(env.depot_file).replaceAll(/.*"contentroot".*/, "\"contentroot\" \"D:\\Builds\\SHS\\${COMBINED_VERSION}\\Windows\\\"")

            writeFile(file: env.depot_file, text: newContent2)
            steam_branch1 =  readFile(env.app_file).replaceAll(/.*"setlive".*/, "\"setlive\" \"${steam_branch}\"") //zmiana steam'owego brancha
            writeFile(file: env.app_file, text: steam_branch1)
            def fileContent3 = readFile(env.depot_file)

            withCredentials([usernamePassword(credentialsId: 'buildmachine_password', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
              bat """D:/Jenkins/steam/sdk/UG/tools/ContentBuilder/builder/steamcmd.exe +login ${USERNAME} "${PASSWORD}" +run_app_build "D:/Jenkins/steam/sdk/UG/tools/ContentBuilder/scripts/app_1452250.vdf" +quit"""
            }
            //bat 'D:/Jenkins/steam/sdk/SHS/tools/ContentBuilder/builder/steamcmd.exe  +login BBG_buildmachine 'buildmachine_password' +run_app_build "D:/Jenkins/steam/sdk/SHS/tools/ContentBuilder/scripts/app_1442430.vdf" +quit"'
            }
        }
      }
    }
    }
  post {
    always {
      logParser failBuildOnError: false, parsingRulesPath: 'D:\\Jenkins\\parser.txt', projectRulePath: 'D:/Jenkins/parser.txt', useProjectRule: false
      script {
        if (skip_build == 'false') {
          dir('C:/Users/Jenkins/AppData/Roaming/Unreal Engine/AutomationTool/Logs/D+Program+Files+Epic+Games+UE_5.4') {
            archiveArtifacts '*.txt'
          }
          bat 'del /q /s "C:\\Users\\Jenkins\\AppData\\Roaming\\Unreal Engine\\AutomationTool\\Logs\\D+Program+Files+Epic+Games+UE_5.4\\*"'
        }
      }
    }
    success {
      script {
        // Wykonaj polecenie i przechwyć wynik
        def p4Result = bat(script: "cmd /c ${gitCommand}", returnStdout: true).trim()
        p4Result = gitResult.split('\n')[-1].trim()

        echo "Wynik komendy p4: ${p4Result}"

        // Filtrowanie linii
        def filteredText = p4Result.replaceAll(/(Change.*|Affected.*|\.\.\..*|D:.*|LogWindows.*|Upgrade.*)\n?/, '')

        // Usunięcie pustych linii
        def nonEmptyLines = filteredText.readLines().findAll { it.trim() != '' }

        // Łączenie przefiltrowanych linii w tekst
        def finalFilteredText = nonEmptyLines.join('\n')
        finalFilteredText = finalFilteredText.replace('#changelist validated', '').trim()

        def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))
        def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}"
        currentBuild.description = "BUILD ID: ${COMBINED_VERSION}_${conf_short}"
        slackSend botUser: true, color: '#00FF00', message: "<@U051VA48N0P><@U040QH00ACU> New $build_conf build available from GIT on $steam_branch steam branch: $P4_CHANGELIST UG Commit description:\n*${finalFilteredText}* <https://drive.google.com/drive/folders/1-82BDcBKSBMVry2T1Lk5XbgO8GSrLSJU?usp=drive_link|UG folder>", tokenCredentialId: 'BuildGoesBRRRRR'
      }
    }
    failure {
      script {
        //def p4Command = "git log --format=%B -n 1"

        // Wykonaj polecenie i przechwyć wynik
        //def p4Result = bat(script: "cmd /c ${p4Command}", returnStdout: true).trim()

       // echo "Wynik komendy p4: ${p4Result}"

        // Łączenie przefiltrowanych linii w tekst

        currentBuild.description = "COMMIT ID: ${commit_short}"
        def filePath = "//GITRUNNER2/jobs//${JOB_NAME}/builds/${BUILD_NUMBER}/logerrorLinks.html"

        def content = new File(filePath).text

        def pattern = /<li>.*?red">/
        def pattern2 = '</span></a></li>'

        def cleanedContent = content.replaceAll(pattern, '')
        cleanedContent = cleanedContent.replaceAll(pattern2, '')
        cleanedContent = cleanedContent.replaceAll(/(LogWindows: Failed to load .*|Upgrade.*)\n?/, '')
        cleanedContent = cleanedContent.readLines().findAll { it.trim() != '' }

        slackSend botUser: true, color: '#FF0000', message: "<@U03RYJLBG90><@U05B1AP5QAY><@U06BXD4LYAE><@U040QH00ACU><@UE4BVMQCT> Build failed at GIT commit: $commit_short  Link to <http://192.168.100.137:8080/job/${JOB_NAME}/${BUILD_NUMBER}/artifact/|artefacts> ${cleanedContent}", tokenCredentialId: 'BuildGoesBRRRRR'
      }
    }
  }
    }
