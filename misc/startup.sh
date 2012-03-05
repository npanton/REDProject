echo `cd /red` || echo "no such directory"
echo `touch in`
echo `touch out`
echo `touch err`
echo `nohup java -jar -Xmx70m REDProject.jar <in >out 2>err &`
# echo `nohup java -jar -Xmx70m REDProject.jar`