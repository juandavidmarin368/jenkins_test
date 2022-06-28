package com.common

import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

import com.common.Echo
import com.common.Shell
import com.common.Security

import groovy.json.JsonSlurperClassic

class DockerBuild implements Serializable {

    def ctx
    String gitRepoName
    String tagedImage
    String dockerRepository
    String pathService
    String optionsDockerInside = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock -v /opt/jenkins_home:/root"

    DockerBuild(ctx,dockerRepository,tagedImage,gitRepoName,pathService){

        this.ctx = ctx
        this.dockerRepository = dockerRepository
        this.tagedImage = tagedImage
        this.gitRepoName = gitRepoName
        this.pathService = pathService

    }

    def buildSpringBootDockerImage(){

        

        def buildDocker = ctx.docker.image("maven:3.6.0-jdk-13")
            buildDocker.inside(this.optionsDockerInside) {
                                
                        ctx.sh "mvn -version"
                        
                        ctx.sh """
                        export DATABASE_NAME=Fitomillonario_2022_02_03
                        export DATABASE_USERNAME=root
                        export DATABASE_PASSWORD=123456
                        export DATABASE_HOST=192.168.0.6
                        export DATABASE_PORT=3306
                        cd ${this.pathService}/source_code && export JAVA_TOOL_OPTIONS='-Duser.home=/root' && mvn clean compile package && mv target/fitomillonario-0.0.1-SNAPSHOT.jar . && 
                        mv src/main/resources/application.properties .
                        """   
        }  
        buildBackendGenericDockerImage()

    }

    def buildVuejsReactJSDockerImage(){

        def buildDocker = ctx.docker.image("node:16")
            buildDocker.inside(this.optionsDockerInside) {
                                
                        ctx.sh "node --version"
                        ctx.sh "cd ${this.pathService}/source_code && npm install && CI='false' && npm run build && mv build ../docker"
            
        }  
        buildFrontendGenericDockerImage()
    }

    def buildwordpressDockerImage(){


    }

    def buildFrontendGenericDockerImage(){

        def buildDocker = ctx.docker.image("builder:v1")
            buildDocker.inside(this.optionsDockerInside) {

                        ctx.sh '''
                        docker rmi -f $(docker images | grep '''+this.dockerRepository+''' | awk '{print $3}') || true        
                        '''         
                        ctx.sh "cd ${this.pathService}/docker && docker build --tag ${this.tagedImage} ."
                    
        }
    }

    def  buildBackendGenericDockerImage(cloud){

        def buildDocker = ctx.docker.image("builder:v1")
            buildDocker.inside(this.optionsDockerInside) {

                        if(cloud.equals("gke")){
                        
                            def script_output = ctx.sh(returnStdout: true, script: """
                            #!/bin/bash
                            set -e
                            set +x
                            value=`docker images | grep """+this.dockerRepository+""" | awk '{print \$3}'`
                            echo "\$value"

                            #ctx.sh "cd ${this.pathService}/docker && docker build --tag ${this.tagedImage} ."
                            
                            """).trim()
                            
                            
                            
                            if(!script_output.equals("")){
                                //ctx.sh(" echo 'IT IS NOT EMPTY'")
                                
                                ctx.sh '''
                                docker rmi -f $(docker images | grep '''+this.dockerRepository+''' | awk '{print $3}') || true        
                                '''
                                ctx.sh "cd ${this.pathService}/source_code && docker build --tag ${this.tagedImage} ."
                            }else{
                                //ctx.sh(" echo 'IT IS EMPTY'")
                                ctx.sh "cd ${this.pathService}/source_code && docker build --tag ${this.tagedImage} ."

                            }
                            //ctx.sh("echo 'herer...'"+script_output)  
                        }
                        if(cloud.equals("aws")){

                            def script_output = ctx.sh(returnStdout: true, script: """
                            #!/bin/bash
                            set -e
                            set +x
                            value=`docker images | grep 129271465998.dkr.ecr.us-east-1.amazonaws.com/backend_cargador_pedidos | awk '{print \$3}'`
                            if [ "${value}" != "" ]; then
                                docker rmi -f $value
                            fi
                            echo \$value
                            """)
                            
                        }
                    
        }
    }

}
