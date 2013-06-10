import socket
import struct
import sys
import signal
#from threading import Thread
import threading
import select
from datetime import datetime
from Queue import Queue

MCAST_PORT = 5007

thread_stop = threading.Event()
ips = []

q = Queue()

def signal_handler(signal, frame):
    print('Stopping Threads...')
    thread_stop.set()
    q.join()
    sys.exit(0)

def printToFile(stop_req):
    f=open("ReceivedEvents", 'a')
    while(not stop_req.is_set()):
        msg = q.get()
        f.write(msg + '\n')
        q.task_done()
    f.close()
    

def listenOn(ip,  port,  name, stop_req):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
#    sock.settimeout(5)
    sock.setblocking(0)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind((ip, port))
    mreq = struct.pack("4sl", socket.inet_aton(ip), socket.INADDR_ANY)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)
 
    try:
    	f=open("ReceivedEvents_" + name, 'a')
    except IOError:
        print("Could not open file!")

    while(not stop_req.is_set()):
        ready = select.select([sock], [], [], 60)
        if ready[0]:
            msg =  str(sock.recv(10240)) + " " + str(datetime.now())
            f.write(msg + '\n')
#	    q.put(msg)
	    
        elif not (ready[0] or ready[1] or ready[2]):
            print("Time out...")
#	    thread_stop.set()
	    f.close()
	    sys.exit(0)

count = 1 #num of threads
threads = []

signal.signal(signal.SIGTERM, signal_handler)

#printingThread = threading.Thread(target=printToFile, args=[thread_stop])

    # Create new threads
name = "Listener_" + str(count)
#    print("sub : " + sub + " " + getIPfromDZ(sub))
listenerThread = threading.Thread(target=listenOn,  args=[sys.argv[1],  MCAST_PORT, name, thread_stop] )
#writerThread = threading.Thread(target=printToFile,  args=[thread_stop] )
#    thread = threading.Thread(target=listenOn,  args=[sys.argv[2],  MCAST_PORT, name, thread_stop] )
listenerThread.start()
#writerThread.start()
threads.append(listenerThread)
#threads.append(writerThread)

q.join()



