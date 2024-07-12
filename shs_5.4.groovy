def change_pin = ''

def SyncWithP4WithClean(InStream, InPin) {
  p4sync charset: 'none',
            credential: 'runner',
            format: 'jenkins-SHS_UG_5_4',
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
        credential: 'runner',
        format: 'jenkins-SHS_UG_5_4',
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
        label 'Unreal'

        customWorkspace 'D:/Jenkins/Workspace/SHS_5_4/'
    }
    }
    environment {
        def SHS_perforce = 'ssl:34.118.20.91:1666'
        def app_file = 'D:/Jenkins/steam/sdk/SHS/tools/ContentBuilder/scripts/app_1442430.vdf'
    def depot_file = 'D:/Jenkins/steam/sdk/SHS/tools/ContentBuilder/scripts/depot_1442431.vdf'
        def depot_file_with_pdb = 'D:/Jenkins/steam/sdk/SHS/tools/ContentBuilder/scripts/depot_with_pdb.vdf'
        def depot_file_without_pdb = 'D:/Jenkins/steam/sdk/SHS/tools/ContentBuilder/scripts/depot_without_pdb.vdf'
        def commit_file = 'D:/Jenkins/Workspace/SHS_5_4_commit_id.txt'
        def skip_build = 'false'
        def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))
        def SUFFIX = '_SHS'
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
          def filePath = "${WORKSPACE}/StorageHunter/Config/DefaultGame.ini"

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
        }
      }
    }

    stage('Check for changes') {
      steps {
        script {
          def triggered_by_user = currentBuild.getBuildCauses()[0].userId
          echo "Build został wywołany przez użytkownika: ${triggered_by_user}."
          def p4commit = "${P4_CHANGELIST}".toInteger()

          def read_commit = readFile(commit_file).trim().toInteger()
          if (triggered_by_user == null && p4commit == read_commit) {
            echo 'Skipping building from the same commit'
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
          def folder_name = "D:/Builds/SHS/${P4_CHANGELIST}_SHS_${conf_short}"
          echo"Nazwa folderu $folder_name"
          if (checkout_clean == 'Yes') {
            bat """
"D:/Program Files/Epic Games/UE_5.4/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -clean -pak -compile -nocompileeditor -installed -project="${WORKSPACE}/StorageHunter/StorageHunter.uproject" -nop4 -utf8output -compileeditor -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig="${env.build_conf}" -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -log="${WORKSPACE}/Logs/SHS_Log.txt"
"""
         }else if (checkout_clean == 'No') {
            bat """
"D:/Program Files/Epic Games/UE_5.4/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -pak -compile -nocompileeditor -installed -project="${WORKSPACE}/StorageHunter/StorageHunter.uproject" -nop4 -utf8output -compileeditor -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig="${env.build_conf}" -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -log="${WORKSPACE}/Logs/SHS_Log.txt"
"""
          }
        }
      }
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

          def build_path = "D:/Builds/SHS/${P4_CHANGELIST}_SHS_${conf_short}/Windows/"
          def compressed_path = "D:/Builds_Compressed/SHS/${P4_CHANGELIST}_SHS_${conf_short}.zip"

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
          echo 'Latest build'
          def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}_${conf_short}"

          def steam_branch = ''
          def newContent =  readFile(app_file).replaceAll(/.*"desc".*/, "\"desc\" \"${COMBINED_VERSION}\"")
          writeFile(file: app_file, text: newContent)

          if (buid_type == 'Latest') {
            steam_branch =  readFile(app_file).replaceAll(/.*"setlive".*/, '\"setlive\" \"unstable\"')
          }

    else if (buid_type == 'From commit') {
            steam_branch =  readFile(app_file).replaceAll(/.*"setlive".*/, '\"setlive\" \"backtracking_builds\"')
    }

          writeFile(file: app_file, text: steam_branch)
          def fileContent2 = readFile(depot_file)

          def newContent2 = readFile(depot_file).replaceAll(/.*"contentroot".*/, "\"contentroot\" \"D:\\\\Builds\\\\SHS\\\\${COMBINED_VERSION}\\\\Windows\\\"")
          writeFile(file: depot_file, text: newContent2)

          def fileContent3 = readFile(depot_file)
          def newContent3 = ''

          if (signfiles_upload == 'No') {
            newContent3 = readFile(depot_file).replaceAll(/.*"FileExclusion".*/, '\"FileExclusion\" \"*.pdb\"')
          }
    else if (signfiles_upload == 'Yes') {
            newContent3 = readFile(depot_file).replaceAll(/.*"FileExclusion".*/, '\"FileExclusion\" \"\"')
    }

          withCredentials([usernamePassword(credentialsId: 'buildmachine_password', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            bat """D:/Jenkins/steam/sdk/SHS/tools/ContentBuilder/builder/steamcmd.exe +login ${USERNAME} "${PASSWORD}" +run_app_build "D:/Jenkins/steam/sdk/SHS/tools/ContentBuilder/scripts/app_1442430.vdf" +quit"""
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
        def p4Command = "p4 -u pkoziol -p ${SHS_perforce} describe -s ${P4_CHANGELIST}"
        def p4Result = bat(script: "cmd /c ${p4Command}", returnStdout: true).trim()

        echo "Wynik komendy p4: ${p4Result}"
        def filteredText = p4Result.replaceAll(/(Change.*|Affected.*|\.\.\..*|D:.*)\n?/, '')

        def nonEmptyLines = filteredText.readLines().findAll { it.trim() != '' }
        def finalFilteredText = nonEmptyLines.join('\n')

        finalFilteredText = finalFilteredText.replace('#changelist validated', '').trim()

        def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}_${conf_short}"
        currentBuild.description = "BUILD ID: ${COMBINED_VERSION}"

        if (buid_type == 'Latest') {
          slackSend botUser: true, channel: '#storage-ci-cd', color: '#00FF00', message: "<@U051VA48N0P> New ${build_conf} build available on unstable steam branch $P4_CHANGELIST SHS Commit description:\n*${finalFilteredText}* <https://drive.google.com/drive/folders/1W-IR7vW27IQEoXZ0d03u77zOnmlGYrtK?usp=drive_link|SHS folder>", tokenCredentialId: 'Raccoons_Builder'
        }
    else if (buid_type == 'From commit') {
          slackSend botUser: true, channel: '#storage-ci-cd', color: '#00FF00', message: "<@U051VA48N0P> New ${build_conf} build available on  backtracking_builds steam branch $P4_CHANGELIST SHS Commit description:\n*${finalFilteredText}* <https://drive.google.com/drive/folders/1W-IR7vW27IQEoXZ0d03u77zOnmlGYrtK?usp=drive_link|SHS folder>", tokenCredentialId: 'Raccoons_Builder'
    }

      // slackSend botUser: true, channel: '#storage-ci-cd', color: '#00FF00', message: "<@U051VA48N0P><@U0525KMA6HW> New build available: $P4_CHANGELIST SHS Commit description:\n*${finalFilteredText}* <https://drive.google.com/drive/folders/1W-IR7vW27IQEoXZ0d03u77zOnmlGYrtK?usp=drive_link|SHS folder>", tokenCredentialId: 'Raccoons_Builder'
      }
    }
    failure {
      script {
        def p4Command = "p4 -u pkoziol -p ${SHS_perforce} describe -s ${P4_CHANGELIST}"
        def p4Result = bat(script: "cmd /c ${p4Command}", returnStdout: true).trim()

        echo "Wynik komendy p4: ${p4Result}"
        def filteredText = p4Result.replaceAll(/(Change.*|Affected.*|\.\.\..*|D:.*)\n?/, '')

        def nonEmptyLines = filteredText.readLines().findAll { it.trim() != '' }
        def finalFilteredText = nonEmptyLines.join('\n').trim()

        finalFilteredText = finalFilteredText.replace('#changelist validated', '').trim()
        currentBuild.description = "COMMIT ID: ${P4_CHANGELIST}"

        def p4Command2 = "p4 -u pkoziol -p ${SHS_perforce} describe -s ${P4_CHANGELIST} | findstr @"
        def p4Result2 = bat(script: "cmd /c ${p4Command2}", returnStdout: true).trim()

        echo "Komenda bez filtracji ${p4Result2}"
        def submitter = p4Result2.replaceAll(/.*by | on .*/, '').replaceAll(/Workspace.*\n?/, '').replaceAll(/^.*D:.*\n?/, '').trim()

        echo "Wynik filtracji: ${submitter}"
        def content = readFile(file: "C:/ProgramData/Jenkins/.jenkins/jobs/${JOB_NAME}/builds/${BUILD_NUMBER}/logerrorLinks.html")

        def pattern = /<li>.*?red">/
        def pattern2 = '</span></a></li>'

        def cleanedContent = content.replaceAll(pattern, '')
        cleanedContent = cleanedContent.replaceAll(pattern2, '')
        cleanedContent = cleanedContent.replaceAll(/(LogWindows: Failed to load .*|Upgrade.*)\n?/, '')
        cleanedContent = cleanedContent.readLines().findAll { it.trim() != '' }

        slackSend botUser: true, channel: '#storage-ci-cd', color: '#FF0000', message: "<@U051VA48N0P><@U03RYJLE7CJ><@U05QKN8475Y>Build failed at commit: $P4_CHANGELIST Commit description:\n*${finalFilteredText}*\n submitted by *${submitter}* Link to <http://192.168.100.137:8080/job/${JOB_NAME}/${BUILD_NUMBER}/artifact/|artefacts> ${cleanedContent}", tokenCredentialId: 'Raccoons_Builder'

      }
    }
  }
    }
