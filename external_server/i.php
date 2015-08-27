<?php
	$dayValue = $_GET['d'];
	$solarValue = $_GET['s'];
	$consValue = $_GET['c'];
	$lightValue = $_GET['l'];
	$username = "beegee_admin";
	$password = "teresa1963";

	try {
		$dbh = new PDO('mysql:host=localhost;dbname=beegee_sp',  $username, $password)or die("cannot open database");
		$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

		$query = "INSERT INTO s (d, s, c, l) VALUES('$dayValue','$solarValue','$consValue','$lightValue')";
		$sth = $dbh->query($query);
	} catch(PDOException $e) {
		echo 'ERROR: ' . $e->getMessage();
	}
?>
