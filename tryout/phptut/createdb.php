<body>
<script language="php">
print "In het begin";

$username="root";
$password="";
$database="database";

$user="root";
$password="";
$database="database";
mysql_connect("localhost",$user,$password);
@mysql_select_db($database) or die( "Unable to select database");
$query="CREATE TABLE contacts (id int(6) NOT NULL auto_increment,first varchar(15) NOT NULL,last varchar(15) NOT NULL,phone varchar(20) NOT NULL,mobile varchar(20) NOT NULL,fax varchar(20) NOT NULL,email varchar(30) NOT NULL,web varchar(30) NOT NULL,PRIMARY KEY (id),UNIQUE id (id),KEY id_2 (id))";
mysql_query($query) or die("Cannot exec create table");
mysql_close();
print "Aan het einde";
</script>
</body>
