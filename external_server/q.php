<?php
/*
 * Connect to SQLite database returning results as JSON
 * START SQLite Section
 */
$username = "beegee_admin";
$password = "teresa1963";
 
$dbh = new PDO('mysql:host=localhost;dbname=beegee_sp',  $username, $password)or die("cannot open database");
$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

// Get year limiter
// limiter is send by URL/query.php?date=yy&get=all
// possible formats
// yy- ~> all from year yy
// yy-mm -> all from year yy and month mm
// yy-mm-dd => all from year yy, month mm and day dd
// yy-mm-dd-hh => all from year yy, month mm, day dd and hour hh
// yy-mm-dd-hh:MM => all from year yy, month mm, day dd, hour hh and minute MM
// if get=all then get all newer rows as well
// Example:
// date=13-08-16-20&get=all returns the rows of 13-08-16 at 20 o'clock and all rows after that
// date=13-08-16-20 returns only the rows of 13-08-16 from 20:00 until 23:59 but not of the next day
// or
// date=13-08-16&get=all returns the rows of 13-08-16 and all rows after that
// date=13-08-16 returns only the rows of 13-08-16 from 00:00 to 23:59 but not of the next day
//
$dateSelect = $_GET['date'];
$syncRest = $_GET['get'];

// Define your SQL statement
$query = "SELECT * FROM s";

if (!empty($dateSelect)) {
	$query = $query . " WHERE d LIKE '" . $dateSelect . "%'";
}

$sth = $dbh->query($query);

$json = array();
$lastID = 0;
while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
	$json[] = $row;
};

$lastIndex = sizeof($json);
$lastID = $json[$lastIndex-1]['id'];
if (!empty($syncRest)) {
	$lastIndex = sizeof($json);
	$lastID = $json[$lastIndex-1]['id'];

	$query = "SELECT * FROM s where id>" . $lastID;

	$sth = $dbh->query($query);

	while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
		$json[] = $row;
	};
}

echo json_encode($json);
?>
