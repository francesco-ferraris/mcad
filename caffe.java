//CON SEMAFORI

int A_in = 0;
int R_in = 0;
int attesa_A = 0;
int attesa_R = 0;
int attesa_estrai = 0;
int max_A = 2/3*Max;
int max_R = 1/3*Max;

sem mutex = 1;
sem A = 0;
sem R = 0;
sem estrai = 0;

Process Versatore_A {

	<..produci A..>
	versa_A();

	public void versa_A() {
		P(mutex);
		if (A_in == Max_A) {
			attesa_A++;
			V(mutex);
			P(A);
			attesa_A--;
		}
		<...guarda la bella barista mentre versa la varietà arabica...>
		A_in++;
		if (attesa_A > 0 && A_in < Max_A)
			V(A);
		else if (attesa_B > 0 && B_in < Max_B)
			V(B);
		else if (attesa_estrai > 0 && A_in == 2*B_in && A_in - B_in >= C) //Se ci sono estrattori bloccati && l'invariante è rispettato && il numero di miscele basta a riempire un sacco
			V(estrai);
		else
			V(mutex);
	}
}

Process Versatore_B {

	<..produci B..>
	versa_B();

	public void versa_B() {
		P(mutex);
		if ((attesa_A >0 && A_in < Max_A) || B_in == Max_B) {
			attesa_B++;
			V(mutex);
			P(B);
			attesa_B--;
		}
		<...guarda la bella barista mentre versa la varietà robusta...>
		B_in++;
		if (attesa_B > 0 && B_in < Max_B)
			V(B);
		else if (attesa_A > 0 && A_in < Max_A)
			V(A);
		else if (attesa_estrai > 0 && A_in == 2*B_in && A_in - B_in >= C) //Se ci sono estrattori bloccati && l'invariante è rispettato && il numero di miscele basta a riempire un sacco
			V(estrai);
		else
			V(mutex);
	}
}

Process Estrattore {
	estrai();

	public void estrai() {
		P(mutex);
		if (A_in != 2*B_in || A_in - B_in < C) {
			attesa_estrai++;
			V(mutex);
			P(estrai);
			attesa_estrai--;
		}
		<...guarda la bella barista mentre estrae C miscela...>
		A_in = A_in - 2*C;
		B_in = B_in - C;
		if (attesa_estrai > 0 && A_in - B_in >= C)
			V(estrai);
		else if (attesa_A > 0)
			V(A);
		else if (attesa_B > 0)
			V(B);
		else
			V(mutex);
	}
}

//CON MONITOR
//I processi sono uguali alla soluzione con semafori.

monitor Cisterna {
	int A_in = 0;
	int R_in = 0;
	int max_A = 2/3*Max;
	int max_R = 1/3*Max;

	condition A;
	condition B;

	public void versa_A() {
		if (A_in == Max_A)
			wait(A);

		<...guarda la bella barista mentre versa la varietà arabica...>
		A_in++;

		if (!empty(A) && A_in < Max_A)
			signal(A);
		else if (!empty(B) && B_in < Max_B)
			signal(B);
		else if (A_in == 2*B_in && A_in - B_in >= C) //Se ci sono estrattori bloccati && l'invariante è rispettato && il numero di miscele basta a riempire un sacco
			signal(estrai);
	}

	public void versa_B() {
		if ((!empty(A) && A_in < Max_A) || B_in == Max_B)
			wait(B);

		<...guarda la bella barista mentre versa la varietà robusta...>
		B_in++;

		if (!empty(B) && B_in < Max_B)
			signal(B);
		else if (!empty(A) && A_in < Max_A)
			signal(A);
		else if (A_in == 2*B_in && A_in - B_in >= C) //Se ci sono estrattori bloccati && l'invariante è rispettato && il numero di miscele basta a riempire un sacco
			signal(estrai);
	}

	public void estrai() {
		if (A_in != 2*B_in || A_in - B_in < C)
			wait(estrai);

		<...guarda la bella barista mentre estrae C miscela...>
		A_in = A_in - 2*C;
		B_in = B_in - C;

		if (!empty(estrai) && A_in - B_in >= C)
			signal(estrai);
		else if (!empty(A))
			signal(A);
		else
			signal(B);
	}
}

//CON SCAMBIO DI MESSAGGI ASINCRONI

Process Versatore_A {
	port signal OK_versa;
	signal s;
	A a;
	process cont;

	<..produci A..>
	send(s) to cont.richiesta_A;
	cont = receive(s) from OK_versa;
	send(a) to cont.a;
}

Process Versatore_B {
	port signal OK_versa;
	signal s;
	B b;
	process cont;

	<..produci B..>
	send(s) to cont.richiesta_B;
	cont = receive(s) from OK_versa;
	send(b) to cont.b;
}

Process Estrattore {
	port Miscela dati;
	signal s;
	process cont;

	send(s) to cont.richiesta_estrai;
	cont = receive(dati) from dati;
}

Process Contenitore {
	port signal richiesta_A, richiesta_B, richiesta_estrai;
	port A a;
	port B b;
	process vers_A, vers_B, estrai;
	queue attesa_A, attesa_B, attesa_estrai;

	do
		- (A_in < Max_A) vers_A = receive(s) from richiesta_A;
					     send(s) to vers_A.OK_versa;
					     vers_A = receive(a) from a;
					     A_in++;
		- (B_in < Max_B && (!empty(attesa_A) || A_in == Max_A) vers_B = receive(s) from richiesta_B;
						 send(s) to vers_B.OK_versa;
						 vers_B = receive(b) from a;
					     B_in++;
		- (A_in == 2*B_in && A_in - B_in >= C) estrai = receive(s) from richiesta_estrai
						 <...estrai C miscela...>
						 send(misc) to estrai.OK_versa;
						 A_in = A_in - 2*C;
						 B_in = B_in - C;
						 if (!empty(estrai) && A_in - B_in >= C)
						 	while(!empty(estrai) && A_in - B_in >= C) {
						 		<...estrai C miscela...>
						 		send(misc) to attesa_estrai.get().dati;
						 		A_in = A_in - 2*C;
						 		B_in = B_in - C;
							}
						 else if (!empty(attesa_A))
						 	while(!empty(attesa_A) && A_in < Max_A) {
								send(s) to vers_A.OK_versa;
								vers_A = receive(a) from a;
					     		A_in++;
							}
						 else if (!empty(attesa_B))
						 	while(!empty(attesa_B) && B_in < Max_B) {
								send(s) to vers_B.OK_versa;
								vers_B = receive(b) from b;
								B_in++;
							}


		- (A_in == Max_A)vers_A = receive(s) from richiesta_A;
						 attesa_A.put(vers_A);
		- (B_in == Max_B)vers_B = receive(s) from richiesta_B;
						 attesa_B.put(vers_B);
		- (A_in != 2*B_in || A_in - B_in < C) estrai = receive(s) from richiesta_estrai
						 attesa_estrai.put(estrai);
}