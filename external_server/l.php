<?php
/*
 * Connect to SQLite database returning results as JSON
 * START SQLite Section
 */
include ("utility.php");

$dbh = new PDO('mysql:host=localhost;dbname=giesecke_sp',  $username, $password)or die("cannot open database");
$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

// Get last entry
//

// Define your SQL statement
$query = "SELECT * FROM s ORDER BY id DESC LIMIT 1";

$sth = $dbh->query($query);

$json = array();
while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
	$json[] = $row;
};

echo json_encode($json);
?>
