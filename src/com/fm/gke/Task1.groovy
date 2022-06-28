package com.fm.gke

import com.common.Security
import com.common.GCPApi
import com.common.Ref
import com.common.DockerBuild
import groovy.json.JsonSlurperClassic
import com.cloudbees.groovy.cps.NonCPS
import org.jenkinsci.plugins.docker.workflow.*
import groovy.json.JsonSlurper
import com.common.Shell
import com.common.Echo

class Task1 implements Serializable{

    private static final long serialVersionUID
    private Shell shell
    private Echo echo

    Security security
    GCPApi gcpApi
    String githubSecretKey
    def ctx
    String gcloud_gcp_key
    String gcloud_service_account
    String version_secret_manager
    boolean isEcrImage
    String environment
    String tagedImage
    String dockerRepository
    String serviceName
    String gitRepoUrl
    String gitRepoName
    String serviceType
    DockerBuild dockerBuild
    String pathService
    String dbname
    String dbhost
    String dbusername
    String dbpassword
    String dbport
    String kubeconfig
    
    Task1(ctx,environment){
        this.ctx = ctx
        this.environment = environment
        this.security = new Security(ctx)
         //CREATE A SECRET TEXT IN JENKINS CREDENTIALS, AND GIVE IT AN ID TO GET IT, IN THIS CASE THE ID WAS FM_ACCESS_KEY

        
    }

    Task1(ctx,environment,serviceName,gitRepoUrl,serviceType,dbname,version_secret_manager) {
        this.ctx = ctx
        
        this.shell = new Shell(ctx)
        this.echo = new Echo(ctx)

        this.environment = environment
        this.security = new Security(ctx)
        this.gcloud_gcp_key         = security.findCredentialSecret("GCLOUD_GCP_KEY")
        this.gcloud_service_account = security.findCredentialSecret("GCLOUD_SERVICE_ACCOUNT")
        this.version_secret_manager = version_secret_manager
  
        this.isEcrImage = false    
        this.gcpApi = new GCPApi(ctx)
        this.tagedImage = Ref.REPOSITORIES."gcloud".repository+"/"+serviceName+":"
        this.dockerRepository = Ref.REPOSITORIES."gcloud".repository+"/"+serviceName
        
        this.serviceName = serviceName
        this.gitRepoUrl = gitRepoUrl
        this.serviceType = serviceType
        this.dbname = dbname
        
    }


    def gcloudCli(){

        //def accessKey = new JsonSlurperClassic().parseText(security.findCredentialSecret("gcloud"))
        //def accessKey = security.findCredentialSecret("gcloud")  
        ctx.println("READY TO PLAY WITH GCLOUD!!")
        ctx.sh "echo '${this.gcloud_gcp_key}'"

        ctx.cleanWs()     
        String temp = """\
            #!/bin/bash -ex
            set +x
                
            echo '${this.gcloud_gcp_key}' >> gcp-key.json     
            
            """.stripIndent() 
            ctx.sh temp

        ctx.sh "mkdir gcloud"
    }


    def checkoutBranch(){

            String branchName=""
            if(this.environment=="prod"){
                branchName = "master"
            }
            if(this.environment=="uat"){

                branchName = "uat"
            }
            if(this.environment=="dev"){

                branchName = "dev"
            }
            
            
            //ctx.cleanWs()   
            String temp = """\
            #!/bin/bash -ex
            set +x
            mkdir -p ~/.ssh    
            echo "Host *" > ~/.ssh/config     
            echo " StrictHostKeyChecking no" >> ~/.ssh/config
            echo "${this.githubSecretKey}" > aws_terraform.pem
            chmod 400 aws_terraform.pem       
            eval "\$(ssh-agent -s)" && ssh-add aws_terraform.pem
            git clone --branch ${branchName} ${this.gitRepoUrl}
            pwd
            ls   
            """.stripIndent() 
            ctx.sh temp
            this.checkIfImageExists();
    }


    def checkIfImageExists(){
        this.gitRepoName = this.getRepoName(this.gitRepoUrl)
        this.pathService = this.gitRepoName+"/"+this.serviceType

        def path = ctx.sh(returnStdout: true, script: """
                            #!/bin/bash
                            pwd
                            """).trim()

        String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock -e CLOUDSDK_CONFIG=${path}/gcloud -v ${path}/gcloud/:${path}/gcloud/ -v ${path}/gcp-key.json:${path}/gcp-key.json"
        def buildDocker = ctx.docker.image("google/cloud-sdk:latest")
            buildDocker.inside(options) {

                //Map result = gcpApi.initialize()
                String serviVersion = this.gcpApi.getServiceVersion(this.pathService)
                this.tagedImage = this.tagedImage+serviVersion
                this.isEcrImage = gcpApi.findContainerRegistryImage(this.dockerRepository, serviVersion)
                if(this.isEcrImage){
                    ctx.println("THE IMAGE EXIST!!")
                }else{
                    ctx.println("THre is not any image")    
                }
          }  

    }

    String getRepoName(String repoSource){
            
            def (value1, value2) = repoSource.tokenize( '/' )
            def (folderName, v2) = value2.tokenize( '.' )
        
            return folderName
    }

    def gettingSecretKey(){

            ctx.cleanWs()    

            
            String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock -v /opt/jenkins_home:/root"

            def buildDocker = ctx.docker.image("builder:v1")
            buildDocker.inside(options) {

                Map result = awsApi.initialize()
                //ctx.println(result)
                
                result = awsApi.findPlainTextSecretFromSecretsManager(Ref.GITHUB_AWS_ARN_SECRET_MANAGER."${environment}".arn,this.environment)         
    
                //********NOTE: TO CONVERT A STRING TO JSON OBJECT WE CAN USE JsonSlurper() OR JsonSlurperClassic() AND CONVERT STRING JSON TO JSON OBJECT
                //AND GET IS VALUE FROM IT
                //def jsonSlurper = new JsonSlurper()
                //def object = jsonSlurper.parseText(result.get('secret'))
                //ctx.println(object.secret_value)    
                    
                def json = new JsonSlurperClassic().parseText(result.get('secret'))
                def secret = json['secret_value']
                
                this.githubSecretKey = secret
                //ctx.println(secret)    

          }  
        
    }
    

    def gettingSecretManager(){

            this.gcloudCli()

            def path = ctx.sh(returnStdout: true, script: """
                            #!/bin/bash
                            pwd
                            """).trim()
    
            
            String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock -e CLOUDSDK_CONFIG=${path}/gcloud -v ${path}/gcloud/:${path}/gcloud/ -v ${path}/gcp-key.json:${path}/gcp-key.json"

            def buildDocker = ctx.docker.image("google/cloud-sdk:latest")
            
            buildDocker.inside(options) {

                Map result = gcpApi.findPlainTextSecretFromSecretsManager(this.gcloud_service_account, this.environment, path, this.version_secret_manager)
                     
                this.githubSecretKey = result.get('secret')

          }  

  
    }

    
    def getKubernetesConfig(){

            ctx.cleanWs()
            String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock -v /opt/jenkins_home:/root"
            def buildDocker = ctx.docker.image("builder:v1")
            
            buildDocker.inside(options) {

                Map result = awsApi.initialize()       
                result = awsApi.findPlainSystemManagerParameterStore_ssm(Ref.KUBE_CONFIG_KUBERNETES."${this.environment}".config,this.environment)         
                this.kubeconfig = result.get('secret')
          }  

        return this.kubeconfig
    }
    
    def gettingDBCredentials(){

           ctx.cleanWs()
           def json = getDBCredentials(this.environment)
              
           this.dbhost     = json['host']
           this.dbusername = json['username']
           this.dbpassword = json['password']
           this.dbport     = json['port']
    }

    def getDBCredentials(String env){

            String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock -v /opt/jenkins_home:/root"
            def buildDocker = ctx.docker.image("builder:v1")
            def json
            buildDocker.inside(options) {

                Map result = awsApi.initialize()
                //ctx.println(result)
                
                result = awsApi.findPlainTextDBCredentials(Ref.RDS_AWS_ARN_SECRET_MANAGER."${env}".arn,env)         

                json = new JsonSlurperClassic().parseText(result.get('secret'))
              
                
          }  

        return json
    }

    //@NonCPS
    def buildAndTagDockerImage(String imageType) {
        
        this.dockerBuild = new DockerBuild(ctx,this.dockerRepository,this.tagedImage,this.gitRepoName,this.pathService)

        if(!isEcrImage){

            if(imageType=="SpringBoot"){


                //ctx.println("READY TO BUILD SPRINGBOOT")
                this.dockerBuild.buildSpringBootDockerImage()

            }
            if(imageType=="Python"){

                this.dockerBuild.buildBackendGenericDockerImage("gke")
            }
            if(imageType=="VuejsReactJS"){

                this.dockerBuild.buildVuejsReactJSDockerImage()
            }
            if(imageType=="Wordpress"){


            }
            
            

        }else{

             ctx.println("there was not any buildAndTagDockerImage because isEcrImage")
         } 



        try {
                //String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock"
                String terraformImage = "nexus.fitomillonario.com/repository/docker-private:v1"
                //docker.withRegistry(VwRef.NEXUS_REGISTRY) {
                //docker.withRegistry("https://nexus.fitomillonario.com") {    
                   //def buildDocker = docker.image(Ref.TERRAFORM_IMAGE)

                    /*
                    timeout(buildTimeoutMinutes) {
                        buildDocker.inside(options) {
                            stage('Sign') {
                                sh """echo 'from new container'"""
                                
                                /*Map result = awsApi.initialize() 
                                 result = awsApi.ecrLogin("dev")
                                 if (result['exitcode'] != 0) {
                                      return result
                                 }*/
                                // vwPemSecret = result['secret']

                                //UpdateBundle updateBundle = new UpdateBundle(this, build, updPath, deleteUnsignedUpd, reason, bundleVersion)
                                //Map result = updateBundle.sign()
                                //build.handleResult(result)
                        //    }
                      //  }
                    //}*/
                //} 
            }catch (Exception x) {
                println("exeption 1")
                //build.handleException(x)
            } finally {
                //notification.send(build)
                println("error 2")
            }


    }


    def pushImageToContainerRegistry(){

        if(!isEcrImage){
           def path = ctx.sh(returnStdout: true, script: """
                            #!/bin/bash
                            pwd
                            """).trim()
    
            
            String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock -e CLOUDSDK_CONFIG=${path}/gcloud -v ${path}/gcloud/:${path}/gcloud/ -v ${path}/gcp-key.json:${path}/gcp-key.json"

            def buildDocker = ctx.docker.image("google/cloud-sdk:latest")
            buildDocker.inside(options) {

                //ctx.sh "node --version"
                //ctx.ssh """
                //    docker --version
                //"""
             
                /*if (result['exitcode'] == EXITCODE_SUCCESS) {         // 0
                    return handleSuccess(result, addNotes)
                } else if (result['exitcode'] >= EXITCODE_ABORT) {    // 1400+
                    return handleAbort(result)
                } else if (result['exitcode'] >= EXITCODE_UNSTABLE) { // 1300-1399
                    return handleUnstable(result)
                } else if (result['exitcode'] >= EXITCODE_WARNING) {  // 1200-1299
                    return handleWarning(result)
                }*/
                Map result = gcpApi.containerRegistryLogin(path)
                //ctx.println(result)  

                /* pushEcrImage
                result = awsApi.tagAndPushEcrImage("dev","backend_cargador_pedidos:0.0.14","0.0.14")
                ctx.println(result) */

                result = gcpApi.pushImageToContainerRegistry(tagedImage)
                ctx.println(result)

                
                if(result.get('exitcode')==0){
                    
                    /*ctx.sh(""" echo 'here equals 0'
                    """)*/
                    ctx.sh '''
                    docker rmi -f $(docker images | grep '''+this.dockerRepository+''' | awk '{print $3}') || true        
                    ''' 
                    ctx.cleanWs()
                }
                      

          }  
          
        }else{

             ctx.println("there was not any buildAndTagDockerImage because isEcrImage")
         } 
  

    }
    

    def updateFluxCDrepository(){
        
            
            gettingSSMSecretKey(this.environment)
            ctx.cleanWs() 
            String githubRepo = Ref.GITHUB_REPOSITORIES_FLUXCD."${this.environment}".fluxcd
            String fluxcdRepoFolder = getRepoName(githubRepo)
            String githubUsername = Ref.GITHUB_SSH_KEY_SECRET_MANAGER."${this.environment}".username
            String branchName =  Ref.GITHUB_REPOSITORIES_FLUXCD."${this.environment}".branchName    

    
            String options = "-u 0 -v /var/run/docker.sock:/var/run/docker.sock"

            def buildDocker = ctx.docker.image("ansible:v3")
            buildDocker.inside(options) {

            int exitcode = shell.execForStatus("""
                    #!/bin/bash -ex
                    set +x
                    mkdir -p ~/.ssh     
                    echo "Host *" > ~/.ssh/config     
                    echo " StrictHostKeyChecking no" >> ~/.ssh/config
                    echo "${this.githubSecretKey}" > aws_terraform.pem
                    chmod 400 aws_terraform.pem       
                    eval "\$(ssh-agent -s)" && ssh-add aws_terraform.pem
                    git clone ${githubRepo}
                    pwd
                    ls 

                    export image_tag=${this.tagedImage}
                    export rds_endpoint=\\"${this.dbhost}\\"
                    export rds_port=\\"${this.dbport}\\"
                    export rds_username=\\"${this.dbusername}\\"
                    export rds_db_name=\\"${this.dbname}\\"
                    export sealedsecret="\$(echo -n '${this.dbpassword}' | base64)"
                    export servicetype=${this.serviceType}
                    export appname="${this.gitRepoName}"
                    export environment="${this.environment}"
                    cd ${fluxcdRepoFolder}
                    
                     
                    ansible-playbook playbook.yaml
                    
                    git add .
                    git config user.email "${githubUsername}"
                    git commit -m "updated ${this.gitRepoName} - ${this.serviceType} - \$(date +"%Y-%m-%d %T")"
                    git push origin ${branchName} 
                      

            """)
            String message = exitcode == 0 ? "SUCCESS" : "failed to commit and push changes" 
            echo.exit msg: "GIT::commitAndPush: ${message}", exitcode: exitcode

                /*Map result = awsApi.initialize()
                //ctx.println(result)
                
                result = awsApi.findPlainTextSecretFromSecretsManager(Ref.GITHUB_AWS_ARN_SECRET_MANAGER."${environment}".arn,this.environment)         
    
  
                def json = new JsonSlurperClassic().parseText(result.get('secret'))
                def secret = json['secret_value']
                
                this.githubSecretKey = secret
                //ctx.println(secret)*/



          } 

    }

}


/*
When saving state on classes, such as above, the class must implement the Serializable interface. This ensures that a Pipeline using the class, as seen in the example, can properly suspend and resume in Jenkins.

Not so much a limitation but more of a requirement. Don’t forget to implement java.io.Serializable by any of your classes to avoid issues for pipelines in case the Jenkins server needs to be restarted.

*/