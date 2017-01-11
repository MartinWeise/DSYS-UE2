Reflect about your solution!

Summary:
- DONE

**************************************************************
Lab2
**************************************************************

Stage 1:

chatserver/TcpHandler.java: Das Registrieren von Adressen erfolgt hier beginnend mit dem Root-Nameserver. Bei einem Lookup werden die entsprechenden Adressen ermittelt und ausgegeben.

nameserver/Nameserver.java: Hier erfolgt die Ausgabe der bekannten Zonen, der gespeicherten Adressen und das Beenden des Nameserers.


Stage 2:

client/Client.java: Die erste Nachricht des Authentifizierungsalgorithmus wird hier vorbereitet, RSA verschlüsselt und Base64 codiert an den Server uebermittelt. Die dritte Nachricht wird hier empfangen, und die erhaltene mit client-challenges wird mit der gesendeten verglichen. Bei Übereinstimmung wird die chatserver-challenge AES verschlüsselt und Base64 codiert an den Serer geschickt.

chatserver/TcpHandler.java: Alle Nachrichten werden hier empfangen, Base64 encodiert, RSA oder AES entschlüsselt und entsprechend bearbeitet. Wenn die Authentifizierung beim Vergleich der gesendeten und erhaltenen chatserver-challenges fehlschlägt, so wird beim Server eine entsprechende Nachricht ausgegeben.


Stage 3:

client/TcpMsgHandler.java: Ausgehende Nachrichten werden hier gehashed und der Hash vorne an die Nachricht angehaengt. Eingehende Nachrichten werden vom Hash getrennt und erneut gehashed. Der gelieferte und berechnete Hash werden verglichen.

client/TcpListener.java: Ausgehende Nachrichten werden hier gehashed. Der Hash wird vorne an die Nachricht angehaengt. Eingehende Nachrichten werden ebenfalls gehashed und der berechnete mit dem gelieferten Hash verglichen.


**************************************************************
Lab1
**************************************************************

chatserver:

TcpListener.java: Der Server wartet in diesem Thread auf eingehende Anfragen von Clients und bearbeitet diese im Thread TcpHandler.java

UdpListener.java: Der Server empfaengt hier eingehende Pakete, und bearbeitet diese im Thread UdpHandler.java


client:

TcpListener.java: Der Client empfaengt hier die Antworten des Servers und der anderen Clients und schreibt diese auf den UserResponseStream.

UdpListener.java: Der Client erwartet hier die Pakete vom Server, und bearbeitet diese im Thread UdpHandler.java

TcpMsgListener.java: Wenn sich ein Client registriert, so wartet dieser in diesem Thread auf eingehende private Nachrichten.

TcpMsgHandler.java: Eingehende private Nachrichten werden hier auf den UserResponseStream geschrieben und mit !ack beantwortet.

