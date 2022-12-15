#!/usr/bin/fish

function logs
	set OUTPUT (./preprocess-logs.py slog/*.log | sort -n | string collect)
	for arg in $argv
		set OUTPUT (echo $OUTPUT | rg $arg | string collect)
	end
	echo $OUTPUT | bat --language log
end

function slogs
	logs "smlog" $argv
end

function plogs
	logs "paxoslog" $argv
end
