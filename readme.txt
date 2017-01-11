Reflect about your solution!

Summary:
- DONE

Server:
TcpListener.java: Der Server wartet in diesem Thread auf eingehende Anfragen von Clients und bearbeitet diese im Thread TcpHandler.java
UdpListener.java: Der Server empf�ngt hier eingehende Pakete, und bearbeitet diese im Thread UdpHandler.java


Client: 
TcpListener.java: Der Client empf�ngt hier die Antworten des Servers und der anderen Clients und schreibt diese auf den UserResponseStream.
UdpListener.java: Der Client erwartet hier die Pakete vom Server, und bearbeitet diese im Thread UdpHandler.java
MsgListener.java: Wenn sich ein Client registriert, so wartet dieser in diesem Thread auf eingehende private Nachrichten.
MsgHandler.java : Eingehende private Nachrichten werden hier auf den UserResponseStream geschrieben und mit !ack beantwortet.
TcpMsgHandler.java: Ausgehende Nachrichten werden hier gehashed und der Hash vorne an die Nachricht angehängt. Eingehende Nachrichten werden vom Hash getrennt und erneut gehashed. Der gelieferte und berechnete Hash werden verglichen.
TcpListener.java: Ausgehende Nachrichten werden hier gehashed. Der Hash wird vorne an die Nachricht angehängt. Eingehende Nachrichten werden ebenfalls gehashed und der berechnete mit dem gelieferten Hash verglichen.

