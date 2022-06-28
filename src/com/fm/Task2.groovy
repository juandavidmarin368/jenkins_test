package com.fm

import groovy.json.JsonSlurperClassic

class Task2{

    String temporal = "JUANDAMALO."
    def ctx


    Task2(ctx) {
        this.ctx = ctx
       
    }



    static methodOne(context, variableUno){

       context.sh """
                  echo 'JUANDAMALO - ${variableUno}'
                  """

    } 

    def myEcho(String msg) {
        this.temporal = msg
        ctx.sh"""
                    echo 'hey --- ${msg}'
                """
    }

    def printingReplacedVariable(){

        String v2 = test.replace(".","*")
        ctx.sh """ echo ' -- ${v2}' 
                   """
    }


    def workingPem(){

        ctx.print("FROM new workingPem")
        newmthod();
        def stdout = ctx.sh(
           script: """
            aws --profile "dev" secretsmanager get-secret-value --secret-id "arn:aws:secretsmanager:us-east-2:502379301106:secret:vwpem-WtJdqe"
        """,returnStdout: true)
     
        def json = new JsonSlurperClassic().parseText(stdout)
        def secret = json['SecretString']
        ctx.print(secret)
    }


}