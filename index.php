

<?php
echo "Hostnames on /etc/hosts file";
$output = shell_exec('cat /etc/hosts');
echo "<pre>$output</pre>";
?>