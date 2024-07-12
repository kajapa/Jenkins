def change_pin = ''

def SyncWithP4WithClean(InStream, InPin) {
    p4sync charset: 'none',
            credential: 'BTR_runner',
            format: 'jenkins-PiratesV3-Node1',
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
        credential: 'BTR_runner',
        format: 'jenkins-PiratesV3-Node1',
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
            label 'team_x'
            customWorkspace 'C:/Jenkins/Workspace/PiratesV3/'
        }
    }
    environment {
        def Pirates_perforce = 'ssl:34.118.20.91:1666'
        def app_file = 'C:/Jenkins/steam/sdk/Pirates/tools/ContentBuilder/scripts/app_1482470.vdf'
        def depot_file = 'C:/Jenkins/steam/sdk/Pirates/tools/ContentBuilder/scripts/depot_1482471.vdf'
        def depot_file_with_pdb = 'C:/Jenkins/steam/sdk/Pirates/tools/ContentBuilder/scripts/depot_with_pdb.vdf'
        def depot_file_without_pdb = 'C:/Jenkins/steam/sdk/Pirates/tools/ContentBuilder/scripts/depot_without_pdb.vdf'
        def commit_file = 'C:/Jenkins/Workspace/Pirates_commit_id.txt'
        def skip_build = 'false'
        def conf_short = build_conf.substring(0, Math.min(build_conf.length(), 5))
        def SUFFIX = '_PiratesV3'
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
                        change_pin = "$env.commit_id"
                        if (checkout_clean == 'Yes') {
                            SyncWithP4WithClean(env.p4stream, change_pin)
        }else if (checkout_clean == 'No') {
                            SyncWithP4WithoutClean(env.p4stream, change_pin)
                        }
                    }
                    echo "Build from specific $env.commit_id commit"
                    echo "Ticket ${P4_CHANGELIST}"
                    currentBuild.description = "BUILD ID: ${P4_CHANGELIST} ${build_conf}"
    //def filePath = "D:/Jenkins/Workspace/SHS/StorageHunter/Config/DefaultGame.ini"

                    //bat "attrib -r ${filePath}"
                    // def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}"
                    //def newContent = readFile(filePath).replaceAll('ProjectVersion=.+', "ProjectVersion=${COMBINED_VERSION}")
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
                    def folder_name = "C:/Builds/PiratesV3/${P4_CHANGELIST}_PiratesV3_${conf_short}"
                    echo"Nazwa folderu $folder_name"
                    if (checkout_clean == 'Yes') {
                        bat """
"D:/Program Files/Epic Games/UE_5.3/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -clean -pak -compile -nocompileeditor -installed -project="C:/Jenkins/Workspace/PiratesV3/Pirates/Pirates.uproject" -nop4 -utf8output -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig="${env.build_conf}" -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -log="C:/Jenkins/Workspace/PiratesV3/Logs/Pirates_Log.txt"
"""
         }else if (checkout_clean == 'No') {
                        bat """
"D:/Program Files/Epic Games/UE_5.3/Engine/Build/BatchFiles/RunUAT.bat" BuildCookRun -rocket -pak -compile -nocompileeditor -installed -project="C:/Jenkins/Workspace/PiratesV3/Pirates/Pirates.uproject" -nop4 -utf8output -buildmachine -platform=Win64 -targetplatform=Win64 -clientconfig="${env.build_conf}" -build -cook -stage -verbose -NoSign -NoCodeSign -stagingdirectory="${folder_name}" -log="C:/Jenkins/Workspace/PiratesV3/Logs/Pirates_Log.txt"
"""
                    }
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
                    def build_path = "C:/Builds/PiratesV3/${P4_CHANGELIST}_PiratesV3_${conf_short}/Windows/"
                    def compressed_path = "C:/Builds_Compressed/PiratesV3/${P4_CHANGELIST}_PiratesV3_${conf_short}.zip"

                    bat """
"C:\\Program Files\\7-Zip\\7z.exe" a -mx0 "${compressed_path}" "${build_path}"
"""
    }}
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
                    def steam_branch = ''
                    if (env.buid_type == 'Latest') {
                        echo 'Latest build'

                        def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}_${conf_short}"
                        steam_branch =  readFile(app_file).replaceAll(/.*"setlive".*/, '\"setlive\" \"vertical_slice_2.0 \"')
                        writeFile(file: app_file, text: steam_branch)

                        def newContent =  readFile(app_file).replaceAll(/.*"desc".*/, "\"desc\" \"${COMBINED_VERSION}\"")
                        writeFile(file: app_file, text: newContent)

                        def newContent2 = readFile(depot_file).replaceAll(/.*"contentroot".*/, "\"contentroot\" \"C:\\\\Builds\\\\PiratesV3\\\\${COMBINED_VERSION}\\\\Windows\\\"")

                        writeFile(file: depot_file, text: newContent2)

                        withCredentials([usernamePassword(credentialsId: 'buildmachine_password', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            bat """C:/Jenkins/steam/sdk/Pirates/tools/ContentBuilder/builder/steamcmd.exe +login ${USERNAME} "${PASSWORD}" +run_app_build "C:/Jenkins/steam/sdk/Pirates/tools/ContentBuilder/scripts/app_1482470.vdf" +quit"""
                        }
                    }
                }
            }
        }
        }
    post {
        always {
            script {
                if (skip_build == 'false') {
                    logParser failBuildOnError: false, parsingRulesPath: 'D:\\Jenkins\\parser.txt', projectRulePath: 'D:/Jenkins/parser.txt', useProjectRule: false
                    dir('C:/Users/Jenkins/AppData/Roaming/Unreal Engine/AutomationTool/Logs/D+Program+Files+Epic+Games+UE_5.3') {
                        archiveArtifacts '*.txt'
                    }
                    bat 'del /q /s "C:\\Users\\Jenkins\\AppData\\Roaming\\Unreal Engine\\AutomationTool\\Logs\\D+Program+Files+Epic+Games+UE_5.3\\*"'
                }
            }
        }
        success {
            script {
                def p4Command = "p4 -u mosica -p ${Pirates_perforce} describe -s ${P4_CHANGELIST}"

                // Wykonaj polecenie i przechwyć wynik
                def p4Result = bat(script: "cmd /c ${p4Command}", returnStdout: true).trim()

    //echo "Wynik komendy p4: ${p4Result}"

                // Filtrowanie linii
                def filteredText = p4Result.replaceAll(/(Change.*|Affected.*|\.\.\..*|C:.*)\n?/, '')

                // Usunięcie pustych linii
                def nonEmptyLines = filteredText.readLines().findAll { it.trim() != '' }

                // Łączenie przefiltrowanych linii w tekst
                def finalFilteredText = nonEmptyLines.join('\n')
                finalFilteredText = finalFilteredText.replace('#changelist validated', '').trim()

                def COMBINED_VERSION = "${P4_CHANGELIST}${SUFFIX}_${conf_short}"
                currentBuild.description = "BUILD ID: ${COMBINED_VERSION}"
                slackSend botUser: true, channel: '#pirates-ue5-ci-cd', color: '#00FF00', message: "<@U051VA48N0P><@U0525KMA6HW><@U05UJ2T4UNA> New build available: $P4_CHANGELIST Pirates Commit description:\n*${finalFilteredText}* <https://drive.google.com/drive/folders/137iUm8aJNVYxyNWPS6LGp2AVb6gyQF0S?usp=drive_link|PiratesV3 folder>", tokenCredentialId: 'Pirates_bot'
            }
        }
        failure {
            script {
                def p4Command = "p4 -u mosica -p ${Pirates_perforce} describe -s ${P4_CHANGELIST}"

                // Wykonaj polecenie i przechwyć wynik
                def p4Result = bat(script: "cmd /c ${p4Command}", returnStdout: true).trim()

    //echo "Wynik komendy p4: ${p4Result}"

                // Filtrowanie linii
                def filteredText = p4Result.replaceAll(/(Change.*|Affected.*|\.\.\..*|C:.*)\n?/, '')

                // Usunięcie pustych linii
                def nonEmptyLines = filteredText.readLines().findAll { it.trim() != '' }

                // Łączenie przefiltrowanych linii w tekst
                def finalFilteredText = nonEmptyLines.join('\n')
                finalFilteredText = finalFilteredText.replace('#changelist validated', '').trim()

                currentBuild.description = "COMMIT ID: ${P4_CHANGELIST}"
                //def content = node('Unreal') { readFile(file: "C:/ProgramData/Jenkins/.jenkins/jobs/${JOB_NAME}/builds/${BUILD_NUMBER}/logerrorLinks.html")}

                def filePath = "//GITRUNNER2/jobs//${JOB_NAME}/builds/${BUILD_NUMBER}/logerrorLinks.html"

                    def content = new File(filePath).text
               // def content = node('Unreal') {readFile(file: "C:/ProgramData/Jenkins/.jenkins/jobs/${JOB_NAME}/builds/${BUILD_NUMBER}/logerrorLinks.html")}

                    def pattern = /<li>.*?red">/
                    def pattern2 = '</span></a></li>'

                    def cleanedContent = content.replaceAll(pattern, '')
                    cleanedContent = cleanedContent.replaceAll(pattern2, '')
                    cleanedContent = cleanedContent.replaceAll(/(LogWindows: Failed to load .*|Upgrade.*)\n?/, '')
                    cleanedContent = cleanedContent.readLines().findAll { it.trim() != '' }

                    //Get submitter

                def p4Command2 = "p4 -u mosica -p ${Pirates_perforce} describe -s ${P4_CHANGELIST}| findstr @"
                def p4Result2 = bat(script: "cmd /c ${p4Command2}", returnStdout: true).trim()
                //echo "Komenda bez filtracji ${p4Result2}"

                def submitter = p4Result2.replaceAll(/.*by | on .*/, '').replaceAll(/Workspace.*\n?/, '').replaceAll(/^.*C:.*\n?/, '').trim()
                //echo "Wynik filtracji: ${submitter}"

                slackSend botUser: true, channel: '#pirates-ue5-ci-cd', color: '#FF0000', message: "<@UE4BVMQCT><@U0173MLGL0H><@U0525KMA6HW><@U05UJ2T4UNA> Build failed at commit: $P4_CHANGELIST Commit description:\n*${finalFilteredText}*\n submitted by *${submitter}* Link to <http://192.168.100.137:8080/job/${JOB_NAME}/${BUILD_NUMBER}/artifact/|artefacts> ${cleanedContent}", tokenCredentialId: 'Pirates_bot'
            }
        }
    }
        }
