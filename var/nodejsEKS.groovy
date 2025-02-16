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
            project = configMap.get("project")
            def releaseExists = ""
        }
        stages {
            stage('Read The Version'){
                steps {
                    script{
                        echo sh(returnStdout: true, script: 'env') //checking env properties of the pipeline, which will be created by jenkins automatically
                        def packageJson = readJSON file: 'package.json'
                        appVersion = packageJson.version
                        echo "application version: $appVersion"
                    } 
                }
            }
            stage('Installing Dependencies') {
                steps { //installing nodejs dependencies and printing appVersion
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
                        zip -q -r ${component}-${appVersion}.zip * -x Jenkinsfile -x ${component}-${appVersion}.zip
                        ls -ltr
                    """
                }
            }
            stage('Docker Build'){ //login to ecr and pushing images into ecr which helps in storing docker images
                steps {
                    sh """
                        aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.${region}.amazonaws.com

                        docker build -t ${account_id}.dkr.ecr.${region}.amazonaws.com/${project}-${component}:${appVersion} .

                        docker push ${account_id}.dkr.ecr.${region}.amazonaws.com/${project}-${component}:${appVersion}

                    """

                }
            }
            stage('Deploy'){ //deploying the application by implementing helm kubernetes
                steps { //after the first installment of helm, mention helm upgrade backend . in the pipeline
                    script{ // if we are using groovy script then we must include within the scrip{}
                        releaseExists = sh(script: "helm list -A --short | grep -w ${component} || true", returnStdout:true).trim() //this will verify whether deployment is successful in host-server
                        if(releaseExists.isEmpty()){
                            echo "${component} is not installed yet, first time installation"
                            sh """
                                aws eks update-kubeconfig --region ${region} --name ${project}-dev
                                cd helm
                                sed -i 's/IMAGE_VERSION/${appVersion}/g' values.yaml 
                                helm install ${component} -n ${project} .
                            """
                        }
                        else{
                           echo "${component} is installed yet, first time installation"
                           sh """
                                aws eks update-kubeconfig --region ${region} --name ${project}-dev
                                cd helm
                                sed -i 's/IMAGE_VERSION/${appVersion}/g' values.yaml 
                                helm upgrade ${component} -n ${project} .
                            """ 
                        }
                    } 
                }
            }
            stage('Verify Deploymnet'){ //if deployment is present it is successful, if not it will rollback to previous version of the pipeline
                steps{
                    script{
                        rollbackStatus = sh(script: "kubectl rollout status deployment/backend -n ${project} --timeout=1m || true", returnStdout:true).trim() //this will check the deployment rollout status whether the deployment is success or not
                        if(rollbackStatus.contains('successfully rolled out')){
                            echo "Deployment is successsful"
                        }
                        else {
                            echo "Deployment is failed, performing rollback"
                            if(releaseExists.isEmpty()){ //if true then rollback will not be done, because it is first deployment
                                error "Deployment failed, not able to rollback, since it is first time deployment"
                            }
                            else{ //this will rollback previous  one version back i.e rollback to previous version
                                sh """
                                    aws eks update-kubeconfig --region ${region} --name ${project}-dev
                                    helm rollback backend -n ${project} 0 
                                    sleep 60
                                """
                                rollbackStatus = sh(script: "kubectl rollout status deployment/backend -n ${project} --timeout=1m || true", returnStdout:true).trim()
                                if(rollbackStatus.contains('successfully rolled out')){
                                    error "Deployment is failed, Rollback is successsful"
                                }
                                else{
                                    error "Deployment is failed, Rollback is failed"
                                }
                            }
                        }
                    }
                }
            }
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