Global variables represent a loosely-defined script with little structure. Those scripts usually containing one or many methods and/or variables. Essentially, global variables are just externalized scripts that can be imported into a Jenkinsfile to break down logic. The naming doesn’t fully express its purpose and can lead to issues among team members when talking about shared library terminology.

Class implementations represent the alternative to scripts. They support a much more structured approach to breaking down functionality into packages and classes, a coding approach you are likely already familiar with if you are writing application source code. One of the major benefits of class implementation is the cabilility to declare and download external libraries via Groovy Grape.

Personally, I am not a fan of using global variables. The ability to expose variables with a global scope often times leads to confusion when tracking down its definition and the place in the code that assign new values. Morever, a script is not well-suited for implementing more elaborate logic as it can easily become spaghetti code.

https://bmuschko.com/blog/jenkins-shared-libraries/

https://medium.com/swlh/using-shared-libraries-in-a-jenkins-pipeline-d20206943792



//***********


