def change_pin = ''

def SyncWithP4WithClean(InStream, InPin) {
  p4sync charset: 'none',
            credential: 'UG_runner',
            format: 'jenkins-UG_5_4',
            populate: autoClean(
              delete: false,
              modtime: false,
              parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'],
              pin: "$InPin",
              quiet: true,
              replace: true,
              tidy: false),
            source: streamSource("$InStream")
}

def SyncWithP4WithoutClean(InStream, InPin) {
  p4sync charset: 'none',
        credential: 'UG_runner',
        format: 'jenkins-UG_5_4',
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

pipeline {
    agent {
    label {
        label 'Mechanic'
        customWorkspace 'D:/Jenkins/Workspace/UG_5_4/'
    }
    }
    environment {
    def p4_server = ''
    def app_file = 'D:/Jenkins/steam/sdk/UG/tools/ContentBuilder/scripts/app_1452250.vdf'
    def depot_file = 'D:/Jenkins/steam/sdk/UG/tools/ContentBuilder/scripts/depot_1452251.vdf'
    def depot_file_with_pdb = 'D:/Jenkins/steam/sdk/UG/tools/ContentBuilder/scripts/depot_with_pdb.vdf'
    def depot_file_without_pdb = 'D:/Jenkins/steam/sdk/UG/tools/ContentBuilder/scripts/depot_without_pdb.vdf'
    def SUFFIX = '_UG'
    def commit_file = 'D:/Jenkins/Workspace/UG_P4_commit_id.txt'
    def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))
    def skip_build = 'false'
    }
    stages {
    stage('Checkout') {
      steps {
        script {
            if (buid_type == 'Latest') {
            echo 'Latest build'
            if (checkout_clean == 'Yes') {
              SyncWithP4WithClean(env.p4stream, change_pin)
        } else if (checkout_clean == 'No') {
              SyncWithP4WithoutClean(env.p4stream, change_pin)
            }
      } else if (buid_type == 'From commit') {
            echo "Build from specific $commit_id commit"
            if (checkout_clean == 'Yes') {
              SyncWithP4WithClean(env.p4stream, env.commit_id)
        }else if (checkout_clean == 'No') {
              SyncWithP4WithoutClean(env.p4stream, env.commit_id)
        }//p4sync charset: 'none', credential: 'runner', format: 'jenkins-SHS', populate: forceClean(have: true, parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'], pin: '$commit_id', quiet: true), source: streamSource("$p4stream")
            }
          currentBuild.description = "BUILD ID: ${P4_CHANGELIST} ${build_conf}"
          def filePath = "${WORKSPACE}/UndergroundGarage/Config/DefaultGame.ini"
          // StaĹ‚y sufiks
          bat "attrib -r ${filePath}"
          def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}"

          def newContent = readFile(filePath).replaceAll('ProjectVersion=.+', "ProjectVersion=${COMBINED_VERSION}")
          writeFile(file: filePath, text: newContent)
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
          echo "Build został wywołany przez użytkownika: ${triggered_by_user}."
          def p4commit = "${P4_CHANGELIST}".toInteger()

          def read_commit = readFile(commit_file).trim().toInteger()
          if (triggered_by_user == null && p4commit == read_commit) {
            echo('Skipping building from the same commit')
            skip_build = 'true'
            currentBuild.description = 'SKIPPED'
            currentBuild.result = 'ABORTED'
          }
            writeFile(file: commit_file, text: p4commit.toString())
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
          def folder_name = "D:/Builds/UndergroundGarage/${P4_CHANGELIST}_UG_${conf_short}"
          echo"Nazwa folderu $folder_name"
          if (checkout_clean == 'Yes') {
            bat """
"D:/Program Files/Epic Games/UE_5.4/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -clean -pak -compile -nocompileeditor -installed -project="${WORKSPACE}/UndergroundGarage/UndergroundGarage.uproject" -nop4 -utf8output  -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig="${env.build_conf}" -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -log="${WORKSPACE}/Logs/UG_Log.txt"
"""
         }else if (checkout_clean == 'No') {
            bat """
"D:/Program Files/Epic Games/UE_5.4/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -pak -compile -nocompileeditor -installed -project="${WORKSPACE}/UndergroundGarage/UndergroundGarage.uproject" -nop4 -utf8output  -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig="${env.build_conf}" -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -log="${WORKSPACE}/Logs/UG_Log.txt"
"""
          }
    //bat '"D:/Program Files/Epic Games/UE_5.4/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -compile -compileeditor -installed -nop4 -project="${WORKSPACE}/UndergroundGarage/UndergroundGarage.uproject" -cook -stage -archive -archivedirectory="${WORKSPACE}/UndergroundGarage/UndergroundGarage/Development/x64" -package -clientconfig=%build_conf% -unrealexe=UnrealEditor-Cmd.exe -clean -pak -prereqs -distribution -nodebuginfo -targetplatform=Win64 -build -utf8output -stagingdirectory="D:/Builds/UndergroundGarage/%P4_CHANGELIST%_UG/" -log="${WORKSPACE}/Logs/UG_Log.txt"'
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

          def build_path = "D:/Builds/UndergroundGarage/${P4_CHANGELIST}_UG_${conf_short}/Windows/"
          def compressed_path = "D:/Builds_Compressed/UndergroundGarage/${P4_CHANGELIST}_UG_${conf_short}.zip"

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
            echo 'Latest build'
            def steam_branch1 = ''
            def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}_${conf_short}"

            def newContent =  readFile(env.app_file).replaceAll(/.*"desc".*/, "\"desc\" \"${COMBINED_VERSION}\"")
            writeFile(file: env.app_file, text: newContent)

            def newContent2 = readFile(env.depot_file).replaceAll(/.*"contentroot".*/, "\"contentroot\" \"D:\\\\Builds\\\\UndergroundGarage\\\\${COMBINED_VERSION}\\\\Windows\\\"")

            writeFile(file: env.depot_file, text: newContent2)
            steam_branch1 =  readFile(env.app_file).replaceAll(/.*"setlive".*/, "\"setlive\" \"${steam_branch}\"") //zmiana steam'owego brancha
            writeFile(file: env.app_file, text: steam_branch1)

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
        def p4Command = "p4 -u hdaniel -p ssl:34.65.228.24:1666 describe -s ${P4_CHANGELIST}"

        // Wykonaj polecenie i przechwyć wynik
        def p4Result = bat(script: "cmd /c ${p4Command}", returnStdout: true).trim()

        echo "Wynik komendy p4: ${p4Result}"

        // Filtrowanie linii
        def filteredText = p4Result.replaceAll(/(Change.*|Affected.*|\.\.\..*|D:.*)\n?/, '')

        // Usunięcie pustych linii
        def nonEmptyLines = filteredText.readLines().findAll { it.trim() != '' }

        // Łączenie przefiltrowanych linii w tekst
        def finalFilteredText = nonEmptyLines.join('\n')
        finalFilteredText = finalFilteredText.replace('#changelist validated', '').trim()

        def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}"
        currentBuild.description = "BUILD ID: ${COMBINED_VERSION}_${conf_short}"
        slackSend botUser: true, color: '#00FF00', message: "<@U051VA48N0P><@U040QH00ACU> New $build_conf build available on $steam_branch steam branch: $P4_CHANGELIST UG Commit description:\n*${finalFilteredText}* <https://drive.google.com/drive/folders/1-82BDcBKSBMVry2T1Lk5XbgO8GSrLSJU?usp=drive_link|UG folder>", tokenCredentialId: 'BuildGoesBRRRRR'
      }
    }
    failure {
      script {
        def p4Command = "p4 -u hdaniel -p ssl:34.65.228.24:1666 describe -s ${P4_CHANGELIST}"

        // Wykonaj polecenie i przechwyć wynik
        def p4Result = bat(script: "cmd /c ${p4Command}", returnStdout: true).trim()

        echo "Wynik komendy p4: ${p4Result}"

        // Filtrowanie linii
        def filteredText = p4Result.replaceAll(/(Change.*|Affected.*|\.\.\..*|D:.*)\n?/, '')

        // Usunięcie pustych linii
        def nonEmptyLines = filteredText.readLines().findAll { it.trim() != '' }

        // Łączenie przefiltrowanych linii w tekst
        def finalFilteredText = nonEmptyLines.join('\n')
        finalFilteredText = finalFilteredText.replace('#changelist validated', '').trim()

        currentBuild.description = "COMMIT ID: ${P4_CHANGELIST}"
        def filePath = "//GITRUNNER2/jobs//${JOB_NAME}/builds/${BUILD_NUMBER}/logerrorLinks.html"

        def content = new File(filePath).text

        def pattern = /<li>.*?red">/
        def pattern2 = '</span></a></li>'

        def cleanedContent = content.replaceAll(pattern, '')
        cleanedContent = cleanedContent.replaceAll(pattern2, '')
        cleanedContent = cleanedContent.replaceAll(/(LogWindows: Failed to load .*|Upgrade.*|Change.*)\n?/, '')
        cleanedContent = cleanedContent.readLines().findAll { it.trim() != '' }

    //Get submitter

        def p4Command2 = "p4 -u hdaniel -p ssl:34.65.228.24:1666 describe -s ${P4_CHANGELIST} | findstr @"
        def p4Result2 = bat(script: "cmd /c ${p4Command2}", returnStdout: true).trim()
        echo "Komenda bez filtracji ${p4Result2}"

        def submitter = p4Result2.replaceAll(/.*by | on .*/, '').replaceAll(/Workspace.*\n?/, '').replaceAll(/^.*D:.*\n?/, '').trim()
        echo "Wynik filtracji: ${submitter}"
        slackSend botUser: true, color: '#FF0000', message: "<@U03RYJLBG90><@U06BXD4LYAE><@U040QH00ACU><@UE4BVMQCT> Build failed at commit: $P4_CHANGELIST Commit description:\n*${finalFilteredText}*\n submitted by *${submitter}* Link to <http://192.168.100.137:8080/job/${JOB_NAME}/${BUILD_NUMBER}/artifact/|artefacts> ${cleanedContent}", tokenCredentialId: 'BuildGoesBRRRRR'
      }
    }
  }
    }
