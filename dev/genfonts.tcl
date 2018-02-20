proc bin2hex {bstr8} {

#high
	set high 0
	for {set i 0} {$i<4} {incr i} {
		set bit [string range $bstr8 $i $i]	
		if {$bit eq 1} {
			incr high [expr int(pow(2,$i))] 
		}
	}
#low
	set low 0
	for {set i 4} {$i<8} {incr i} {
		set bit [string range $bstr8 $i $i]	
		if {$bit eq 1} {
			incr low [expr int(pow(2,$i-4))] 
		}
	}

	return "0x[format %01x $low][format %01x $high]"
}

set q [open font8x10.txt r]
set data [read $q]
set lines [split $data "\n"]
array set digits {}
foreach line $lines {
	set len [string length $line]
	set c 0
	if {$len>0} {
		set comp [string map {"1\t" 1} $line]
		set comp [string map {"\t" 0} $comp]
		set len [string length $comp]
		for {set start 0} {$start<$len} { incr start 8 } {		
			set stripe [string range $comp $start [expr $start+8]]
			# replaces tab with 0's then converts to hex decimal
			lappend digits($c) $stripe
			incr c
		}
	}
}

close $q

for {set i 0} {$i<16} {incr i} {
	set t $digits($i)
	puts -nonewline "/*[format %x $i]*/"
	foreach line $t {
		set byte [bin2hex [string trim [string reverse $line]]]
		puts -nonewline "$byte,"
	}
	puts ""
}
