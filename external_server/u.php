<?php
	$dayValue = $_GET['d'];
	$solarValue = $_GET['s'];
	$consValue = $_GET['c'];
	$lightValue = $_GET['l'];

	include ("utility.php");

	try {
		$dbh = new PDO('mysql:host=localhost;dbname=giesecke_sp',  $username, $password)or die("cannot open database");
		$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

		$query = "INSERT INTO s(d, s, c, l) SELECT * FROM (SELECT '$dayValue','$solarValue','$consValue','$lightValue') AS tmp WHERE NOT EXISTS (SELECT d FROM s WHERE d = '$dayValue') LIMIT 1";
		//print_r ("$query");
		$sth = $dbh->query($query);
	} catch(PDOException $e) {
		echo 'ERROR: ' . $e->getMessage();
	}
	print_r ($sth->rowCount());
?>
