## Rules for develop in this project
- Always respect the archtecture, following the SOLID principals
- Always verify with the arch test if no arch rule was broken
- Don't create anemic models, always create rich models, simple and concise
- All the app is divided in application / domain / infrasctructure
- always provide unit tests, write it using spock with groovy. Create tests that really test the flow, don't create dummy tests
- always run all tests before finish the work in progress

## JAVA following pattern
- Dont use 'var' for object declaration, use the right Object reference
- ALWAYS create the IF's like this:
if (sss == sss) {
    ...
}
don't use inline ifs like: 
if (sss == sss) ....
- don't create "spaguetti code", divide in small blocks of function, maximum of 25 lines

## !!!!!!IMPORTANT!!!!! rules
- Don't access any .env file
- Don't commit any file by your own
