<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Try 127.0.0.1 first (more reliable than 'localhost')
$conn = mysqli_connect("127.0.0.1", "root", "687354", "fastable");

// Fallback to localhost if 127.0.0.1 fails
if(!$conn){
    $conn = mysqli_connect("localhost", "root", "687354", "fastable");
}

// If both fail, show error
if(!$conn){
    die(json_encode(array('status' => 0, 'message' => 'Database connection failed: '. mysqli_connect_error())));
}

mysqli_set_charset($conn, "utf8mb4");

// Increase MySQL timeouts for large base64 images
$conn->query("SET SESSION wait_timeout = 600");
$conn->query("SET SESSION interactive_timeout = 600");

// Note: max_allowed_packet is read-only in SESSION scope
// To increase it permanently, edit C:\xampp\mysql\bin\my.ini:
// [mysqld]
// max_allowed_packet = 64M
// Then restart MySQL in XAMPP

// Disable strict mode to prevent errors with default values
$conn->query("SET SESSION sql_mode = ''");
?>

