<?php
/*
 * Connect to SQLite database returning results as JSON
 * START SQLite Section
 */
// Specify your sqlite database name and path
$dir = 'sqlite:/mnt/sda1/s.db';
// Instantiate PDO connection object and failure msg
$dbh = new PDO($dir) or die("cannot open database");

// Define your SQL statement
$query = "SELECT * FROM s";

$sth = $dbh->query($query);

$json = array();
while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
	$json[] = $row;
};
//echo "id,d,s,c,l</br>";
//for ($i=0; $i<count($json); $i++) {
//	echo $json[$i][id].",".$json[$i][d].",".$json[$i][s].",".$json[$i][c].",".$json[$i][l]."</br>";
//}

//$header = '';
//$output = fopen("php://output",'w') or die("Can't open php://output");
//header("Content-Type:application/csv");
//header("Content-Disposition:attachment;filename=spMonitor.csv");
//fputcsv($output, array('id','d','s','c','l'));

//foreach($json as $data){
//    fputcsv($output, $data);
//}
//fclose($output) or die("Can't close php://output");
echo '<html><head><title>spMonitor Save as CSV</title></head><body bgcolor="#000000"><font color="white">';

echo "<p>File ready to be sent, now saving on disk</p>";
$outFile = fopen("/mnt/sda1/db.csv",'w') or die("Can't open /mnt/sda1/db.csv");
fputcsv($outFile, array('id','d','s','c','l'));
foreach($json as $data){
    fputcsv($outFile, $data);
}
fclose($outFile) or die("Can't close /mnt/sda1/db.csv");
echo "<p>File saved</p>";
echo "</font></body></html>";

?>
