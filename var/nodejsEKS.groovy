def call(Map configMap){
    pipeline {
        agent {
            label 'AGENT-1'
        }
        options {
            timeout(time: 30, unit: 'MINUTES') 
            disableConcurrentBuilds()
            ansiColor('xterm')
        }
        environment{
            def appVersion = '' // global variable which can be accessed anywhere within the file
            nexusUrl = pipelineGlobals.nexusURL()
            region = pipelineGlobals.region()
            account_id = pipelineGlobals.account_id()
            component = configMap.get("component")
        }
        stages {
            stage('Read The Version'){
                steps {
                    script{
                        def packageJson = readJSON file: 'package.json'
                        appVersion = packageJson.version
                        echo "application version: $appVersion"
                    } 
                }
            }
            stage('Installing Dependencies') {
                steps {
                    sh """
                        npm install
                        ls -ltr
                        echo "application version: $appVersion"
                    """
                }
            }
            stage('Build'){ // ziping dependencies and version of the backend into a .zip file
                steps { 
                    sh """
                        zip -q -r backend-${appVersion}.zip * -x Jenkinsfile -x backend-${appVersion}.zip
                        ls -ltr
                    """
                }
            }
            stage('Docker Build'){ //login to ecr and pushing images into ecr which helps in storing docker images
                steps {
                    sh """
                        aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.${region}.amazonaws.com

                        docker build -t ${account_id}.dkr.ecr.${region}.amazonaws.com/expense-backend:${appVersion} .

                        docker push ${account_id}.dkr.ecr.${region}.amazonaws.com/expense-backend:${appVersion}

                    """

                }
            }
            stage('Deploy'){ //deploying the application by implementing helm kubernetes
                steps { //after the first installment of helm, mention helm upgrade backend . in the pipeline 
                    sh """
                        aws eks update-kubeconfig --region us-east-1 --name expense-dev
                        cd helm
                        sed -i 's/IMAGE_VERSION/${appVersion}/g' values.yaml 
                        helm install backend .
                    """

                }
            }
            // stage('Sonar Scan') {
            //     environment {
            //         scannerHome = tool 'sonar-6.0' // refering scanner CLI
            //     }
            //     steps {
            //         script {
            //             withSonarQubeEnv('sonar-6.0') { // refering sonar server 
            //                 sh "${scannerHome}/bin/sonar-scanner"
            //             }
            //         }
            //     }
            // }
            // stage("Quality Gate") {
            //     steps {
            //       timeout(time: 30, unit: 'MINUTES') {
            //         waitForQualityGate abortPipeline: true
            //       }
            //     }
            //   }
            // stage('Nexus Artifact Uploader'){ // uploading the backend zip to the nexus repository(backend)
            //     steps {
            //         script {
            //             nexusArtifactUploader(
            //                 nexusVersion: 'nexus3',
            //                 protocol: 'http',
            //                 nexusUrl: "${nexusUrl}",
            //                 groupId: 'com.expense',
            //                 version: "${appVersion}",
            //                 repository: "backend",
            //                 credentialsId: 'nexus-auth',
            //                 artifacts: [
            //                     [artifactId: "backend",
            //                     classifier: '',
            //                     file: "backend-" + "${appVersion}" + '.zip',
            //                     type: 'zip']
            //                 ]
            //              )

            //         }
            //     }
            // }
        //     stage('Deploy'){ //transfering build job backend to backend-deploy and passing appVersion as input to the backend-deploy(pipeline)
        //         when {
        //             expression {
        //                 params.deploy
        //             }
        //         }
        //         steps {
        //             script {
        //                 def params = [
        //                 string(name: 'appVersion', value: "${appVersion}")
        //             ]
        //                 build job: 'backend-deploy', parameters: params, wait: false  // when we include wait:false upstream job won't wait for downstream job
                        
        //             }
        //         }
        //     }
        }
            post { 
            always { 
                echo 'I will always say Hello again!'
                //deleteDir()
            }
            success { 
                echo 'I will run only when pipeline is success'
            }
            failure { 
                echo 'I will run only when pipeline is failure'
            }
        }
        }
    }