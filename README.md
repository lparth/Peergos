![Peergos Logo](https://raw.githubusercontent.com/ianopolous/Peergos/gh-pages/images/PeergosLogo.png)

Peergos
========

Peergos is a peer-to-peer encrypted filesystem with secure sharing of files designed to be resistant to surveillance of data content or friendship graphs. It will have a secure email replacement, with some interoperability with email. There will also be a totally private and secure social network, where users are in control of who sees what (executed cryptographically).

The name Peergos comes from the Greek word Πύργος (Pyrgos), which means stronghold or tower, but phonetically spelt with the nice connection to being peer-to-peer. 

There is a single machine demo running at [https://demo.peergos.net](https://demo.peergos.net). We would very much appreciate any help. 

Peergos aims
------------
 - To allow individuals to securely and privately store files in a peer to peer network which has no central node and is generally difficult to disrupt or surveil
 - To allow secure sharing of such files with other users of the network without visible meta-data (who shares with who)
 - To have a beautiful user interface that any computer or mobile user can understand
 - To have super fast file transfers by transferring fragments in parallel to/from different sources
 - To enable a new secure form of email
 - To be independent of the central SSL CA trust architecture
 - Self hostable - A user should be able to easily run Peergos on a machine in their home and get their own Peergos storage space, and social communication platform from it. 
 - A secure web interface
 - To enable secure real time chat, and video conversations

Project anti-aims
-----------------
 - Peergos does not provide anonymity. Anonymity can be achieved by creating and only ever accessing a User account over Tor

Architecture
------------
1.0 Layers of architecture
 - 1: Peer-to-peer and data layer - [IPFS](https://ipfs.io) provides the data storage, routing and retrieval
 - 2: Authorization Layer - IPNS - controls who is able to modify parts of the file system
 - 3: Distributed file system - Uses erasure codes to add fault tolerance. User's clients are responsible for ensuring that enough fragments of their files survive. 
 - 4: Encryption - Strong encryption is done on the user's machine using [TweetNaCl](http://tweetnacl.cr.yp.to/). 
 - 5: Social layer implementing the concept of following or being friends with another user, without exposing the friend network to anyone.
 - 5: Sharing - Secure cryptographic sharing of files with friends.

2.0 Language
 - The IPFS layer is currently coded in Go
 - The rest is coded to run on JVM to get portability and speed, predominantly Java

3.0 Nodes
 - Types of node in decreasing order of reliability: Core node, 
 - The Core nodes are highly reliable nodes. They store the username <--> public key mapping and the encrypted pending follow requests
 - A new node contacts a publc Peergos server to join the network

4.0 Trust
 - There is a self-signed root certificate used to sign releases, which the user can choose to install
 - A user who trusts a public Peergos server can use the web interface over TLS
 - A less trusting user can run a Peergos server on their own machine and use the web interface over localhost

4.0 Logging in
 - A user's username is salted with the hash of their password and then run through scrypt (with paramters 17, 8, 96, 1000) to generate a symmertic key, an encypting keypair and a signing keypair. This means that a user can log in from any machine without transfering any keys, and also that their keys are protected from a brute force attack.

5.0 Encryption
 - private keys never leave client node
 - encrypted files are duplicated locally, using erasure codes, into multiple fragments to distribute to the network

6.0 Incentives
 - Amount of storage individuals are allowed to use is the amount they donate divided by the replication ratio. This amount takes a week of > 70% up-time to be usable, and will decrease if donated storage is ever online for less than 70% in the previous month (as measured by the network)

7.0 Repair after node disappearance
 - User's client is responsible for ensuring enough fragments of their files remain (another incentive to stay online)
 - For paying users, we can keep a copy of the (encrypted) fragments on our servers to 100% guarantee availability

8.0 Friend network
 - Anyone can send anyone else a "friend request". This amounts to "following" someone and is a one way protocol. This is stored in the core codes, but the core nodes cannot see who is sending the friend request. 
 - The target user can respond to friend requests with their own friend request to make it bi-directional (the usual concept of a friend). 
 - There is no way for the core nodes to deduce the friendship graph (who is friends with who). The plan is to send follow requests over tor, so even an adversary monitoring the network in realtime couldn't deduce the friendship graph

9.0 Sharing of a file (with another user, or publicly)
 - Once user A is being followed by user B, then A can share files with user B (B can revoke their following at any time)
 - based on [cryptree](http://boga.googlecode.com/svn/trunk/res/Docs/wuala-cryptree.pdf) system used by Wuala
 - sharing of a text file with another user could constitute a secure email
 - a public link can be generated to a file or a folder which can be shared with anyone through any medium

Usage
-----
First install and run IPFS

The run with the following to find out available options:
java -jar PeergosServer.jar -help
