package com.common


class Ref {

    static final def REPOSITORIES = [

        fm        : [repository: "129271465998.dkr.ecr.us-east-1.amazonaws.com", region: "us-east-1"],
        cloudguru : [repository: "129271465998.dkr.ecr.us-east-1.amazonaws.com", region: "us-east-1"],
        gcloud    : [repository: "gcr.io/toor-409", region: "us-east-1"],


    ]

    static final def GITHUB_AWS_ARN_SECRET_MANAGER = [

        prod  : [arn: "arn:aws:secretsmanager:us-east-1:129271465998:secret:fitomedics_github-yWqmaD", description: "fitomedics_github_ssh_key"],
        uat   : [arn: "arn:aws:secretsmanager:us-east-1:129271465998:secret:fitomedics_github-yWqmaD", description: "fitomedics_github_ssh_key"],
        dev   : [arn: "arn:aws:secretsmanager:us-east-1:129271465998:secret:fitomedics_github-yWqmaD", description: "juandavid_github_ssh_key"]

    ]

    static final def GITHUB_SSH_KEY_SECRET_MANAGER = [

        prod  : [name: "/prod/fitomedics_github_ssh_key", description: "fitomedics_github_ssh_key", username: "fitomedicssoporte@gmail.com"],
        uat   : [name: "/prod/fitomedics_github_ssh_key", description: "fitomedics_github_ssh_key", username: "fitomedicssoporte@gmail.com"],
        dev   : [name: "/prod/fitomedics_github_ssh_key", description: "fitomedics_github_ssh_key", username: "fitomedicssoporte@gmail.com"],
        sta   : [name: "/dev/juandavid_github_ssh_key", description: "juandavid_github_ssh_key", username: "juandavidmarin368@gmail.com"]

    ]

    static final def RDS_AWS_ARN_SECRET_MANAGER = [

        prod  : [arn: "arn:aws:secretsmanager:us-east-1:129271465998:secret:rds-multiapp-1-6puD0g", description: ""],
        uat   : [arn: "arn:aws:secretsmanager:us-east-1:129271465998:secret:rds-multiapp-1-6puD0g", description: ""],
        dev   : [arn: "arn:aws:secretsmanager:us-east-1:129271465998:secret:rds-multiapp-1-6puD0g", description: "rds_development"]

    ]

    static final def GITHUB_REPOSITORIES_KUSTOMIZE = [
        
        prod : [reponame: "git@github.com:fitomedicsapps/kustomize_manifests.git", description: "", branchName: "master"],
        uat  : [reponame: "git@github.com:fitomedicsapps/kustomize_manifests.git", description: "", branchName: "master"],
        dev  : [reponame: "git@github.com:fitomedicsapps/kustomize_manifests.git", description: "", branchName: "master"]

    ]

    static final def KUBE_CONFIG_KUBERNETES = [
        
        prod : [config: "/prod/kubernetes", description: "", branchName: "master"],
        uat  : [config: "/uat/kubernetes", description: "", branchName: "master"],
        dev  : [config: "/dev/kuberbenetes", description: "", branchName: "master"]

    ]

    static final def GITHUB_REPOSITORIES_TERRAFORM = [

        prod  : [terraform: "git@github.com:juandavidmarin368/terraform_infrastructure.git", description: ""],
        uat  : [terraform: "git@github.com:juandavidmarin368/terraform_infrastructure.git", description: ""],
        dev  : [terraform: "git@github.com:juandavidmarin368/terraform_infrastructure.git", description: ""]

    ]


}    