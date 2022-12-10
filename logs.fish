#!/usr/bin/fish

function logs
	set OUTPUT (cat paxos-*.log | sort -n | string collect)
	for arg in $argv
		set OUTPUT (echo $OUTPUT | rg $arg | string collect)
	end
	echo $OUTPUT | bat --language log
end
