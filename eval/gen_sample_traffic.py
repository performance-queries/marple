from scapy.all import Ether, IP, TCP, sendp
import random

# Generate traffic from random source IPs to destinations:
# x.x.i.j, with j packets to each such destination.
count = 0
for i in xrange(256):
    for j in xrange(256):
        for k in xrange(1):
            if count % 1000 == 0:
                print "%d packets sent" % count
            src = "%d.%d.%d.%d" % (random.randint(0,255), random.randint(0,255), random.randint(0,255), random.randint(0,255))  
            p = Ether() / IP(src=src, dst="0.0.%d.%d" % (i,j), ttl=10) / TCP()
            sendp(p, iface="veth3", verbose=False)
            count = count + 1

