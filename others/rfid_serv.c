/*
 * RFID_SERV is a TCP server that send Tag ID to connected client
 * For PhidgetRFID product
 *
 * Erkan Valentin - 10 May 2012
 * LSIIT/ICube - RP Team (Senslab/FIT project)
 *
 * 0.2
 * */

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <unistd.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/un.h>
#include <phidget21.h>

#define MYPORT 9999
#define BACKLOG 5
#define MAXCLIENTS 2
#define MAXDATASIZE 100

#define MSG_TAGID_LEN 13

//client connected
int clients[MAXCLIENTS];
char tag_id[MSG_TAGID_LEN];

//RFID Attach
int CCONV AttachHandler(CPhidgetHandle RFID, void *userptr)
{
    int serialNo;
    const char *name;

    CPhidget_getDeviceName (RFID, &name);
    CPhidget_getSerialNumber(RFID, &serialNo);
    printf("%s %10d attached!\n", name, serialNo);

    return 0;
}

//RFID Detach
int CCONV DetachHandler(CPhidgetHandle RFID, void *userptr)
{
    int serialNo;
    const char *name;

    CPhidget_getDeviceName (RFID, &name);
    CPhidget_getSerialNumber(RFID, &serialNo);
    printf("%s %10d detached!\n", name, serialNo);

    return 0;
}

//RFID error handler
int CCONV ErrorHandler(CPhidgetHandle RFID, void *userptr, int ErrorCode, const char *unknown)
{
    printf("Error handled. %d - %s\n", ErrorCode, unknown);
    return 0;
}

//RFID output state change
int CCONV OutputChangeHandler(CPhidgetRFIDHandle RFID, void *usrptr, int Index, int State)
{
    if(Index == 0 || Index == 1)
    {
        //printf("Output: %d > State: %d\n", Index, State);
    }
    return 0;
}

//RFID read tag handler
int CCONV TagHandler(CPhidgetRFIDHandle RFID, void *usrptr, unsigned char *TagVal)
{
    //turn on the Onboard LED
    CPhidgetRFID_setLEDOn(RFID, 1);

    //send tag_id to all connected client
    sprintf(tag_id,"R;%02x%02x%02x%02x%02x\n", TagVal[0], TagVal[1], TagVal[2], TagVal[3], TagVal[4]);

    int i = 0;
    for(i=0; i<MAXCLIENTS; i++) {
        if ( clients[i] != 0 ) {
            send(clients[i],tag_id,MSG_TAGID_LEN,MSG_NOSIGNAL);
        }
    }

    printf("Tag Read: %02x%02x%02x%02x%02x\n", TagVal[0], TagVal[1], TagVal[2], TagVal[3], TagVal[4]);
    return 0;
}

//RFID lost tag handler
int CCONV TagLostHandler(CPhidgetRFIDHandle RFID, void *usrptr, unsigned char *TagVal)
{
    //turn off the Onboard LED
    CPhidgetRFID_setLEDOn(RFID, 0);

    //send tag_id to all connected client
    sprintf(tag_id,"L;%02x%02x%02x%02x%02x\n", TagVal[0], TagVal[1], TagVal[2], TagVal[3], TagVal[4]);

    int i = 0;
    for(i=0; i<MAXCLIENTS; i++) {
        if ( clients[i] != 0 ) {
            send(clients[i],tag_id,MSG_TAGID_LEN,MSG_NOSIGNAL);
        }
    }

    printf("Tag Lost: %02x%02x%02x%02x%02x\n", TagVal[0], TagVal[1], TagVal[2], TagVal[3], TagVal[4]);
    return 0;
}

//Display the properties of the attached phidget to the screen.  We will be displaying the name, serial number and version of the attached device.
//We will also display the nu,mber of available digital outputs
int display_properties(CPhidgetRFIDHandle phid)
{
    int serialNo, version, numOutputs, antennaOn, LEDOn;
    const char* ptr;

    CPhidget_getDeviceType((CPhidgetHandle)phid, &ptr);
    CPhidget_getSerialNumber((CPhidgetHandle)phid, &serialNo);
    CPhidget_getDeviceVersion((CPhidgetHandle)phid, &version);

    CPhidgetRFID_getOutputCount (phid, &numOutputs);
    CPhidgetRFID_getAntennaOn (phid, &antennaOn);
    CPhidgetRFID_getLEDOn (phid, &LEDOn);


    printf("%s\n", ptr);
    printf("Serial Number: %10d\nVersion: %8d\n", serialNo, version);
    printf("# Outputs: %d\n\n", numOutputs);
    printf("Antenna Status: %d\nOnboard LED Status: %d\n", antennaOn, LEDOn);

    return 0;
}



int init_rfid(void)
{
    int result;
    const char *err;

    //Declare an RFID handle
    CPhidgetRFIDHandle rfid = 0;

    //create the RFID object
    CPhidgetRFID_create(&rfid);

    //Set the handlers to be run when the device is plugged in or opened from software, unplugged or closed from software, or generates an error.
    CPhidget_set_OnAttach_Handler((CPhidgetHandle)rfid, AttachHandler, NULL);
    CPhidget_set_OnDetach_Handler((CPhidgetHandle)rfid, DetachHandler, NULL);
    CPhidget_set_OnError_Handler((CPhidgetHandle)rfid, ErrorHandler, NULL);

    //Registers a callback that will run if an output changes.
    //Requires the handle for the Phidget, the function that will be called, and an arbitrary pointer that will be supplied to the callback function (may be NULL).
    CPhidgetRFID_set_OnOutputChange_Handler(rfid, OutputChangeHandler, NULL);

    //Registers a callback that will run when a Tag is read.
    //Requires the handle for the PhidgetRFID, the function that will be called, and an arbitrary pointer that will be supplied to the callback function (may be NULL).
    CPhidgetRFID_set_OnTag_Handler(rfid, TagHandler, NULL);

    //Registers a callback that will run when a Tag is lost (removed from antenna read range).
    //Requires the handle for the PhidgetRFID, the function that will be called, and an arbitrary pointer that will be supplied to the callback function (may be NULL).
    CPhidgetRFID_set_OnTagLost_Handler(rfid, TagLostHandler, NULL);

    //open the RFID for device connections
    CPhidget_open((CPhidgetHandle)rfid, -1);

    //get the program to wait for an RFID device to be attached
    printf("Waiting for RFID to be attached....\n");
    if((result = CPhidget_waitForAttachment((CPhidgetHandle)rfid, 10000)))
    {
        CPhidget_getErrorDescription(result, &err);
        printf("Problem waiting for attachment: %s\n", err);
        return 0;
    }

    //Display the properties of the attached RFID device
    //display_properties(rfid);

    CPhidgetRFID_setAntennaOn(rfid, 1);

    //read RFID event data
    printf("Reading.....\n");

    //toggle the digital output (when making this example I had an LED plugged into the digital output index 0
    CPhidgetRFID_setOutputState(rfid, 0, 1);

    //toggle the digital output (when making this example I had an LED plugged into the digital output index 0
    CPhidgetRFID_setOutputState(rfid, 0, 0);;

    //since user input has been read, this is a signal to terminate the program so we will close the phidget and delete the object we created
//	printf("Closing...\n");
//	CPhidget_close((CPhidgetHandle)rfid);
//	CPhidget_delete((CPhidgetHandle)rfid);

    //all done, exit
    return 0;
}

int main(void)
{
    int sockfd,new_fd,numbytes,highest = 0,i;

    char buffer[MAXDATASIZE] ;

    struct sockaddr_in my_addr,their_addr;
    socklen_t sin_size;
    struct timeval tv;
    fd_set readfds;

    //init rfid
    init_rfid();

    //create TCP socket
    if ((sockfd = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
        perror("socket");
        exit(-1);
    }

    int one = 1;
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));

    my_addr.sin_family = AF_INET;
    my_addr.sin_port = htons(MYPORT);
    my_addr.sin_addr.s_addr = INADDR_ANY;
    bzero(&(my_addr.sin_zero), 8);

    //bind socket
    if (bind(sockfd, (struct sockaddr *)&my_addr, sizeof(struct sockaddr)) == -1) {
        perror("bind");
        exit(-1);
    }

    //start listening
    if (listen(sockfd, BACKLOG) == -1) {
        perror("listen");
        exit(-1);
    }

    bzero(clients,sizeof(clients));
    highest = sockfd ;

    while(1) {

        sin_size = sizeof(struct sockaddr_in);
        tv.tv_sec = 0;
        tv.tv_usec = 250000;
        FD_ZERO(&readfds);

        for ( i = 0 ; i < MAXCLIENTS ; i ++ ) {
            if ( clients[i] != 0 ) {
                FD_SET(clients[i],&readfds);
            }
        }

        FD_SET(sockfd,&readfds);

        //non-blocking select
        if (select(highest+1, &readfds, NULL, NULL, &tv) >=0 ) {

            //check select
            if (FD_ISSET(sockfd, &readfds)) {

                //accept the new client
                if ((new_fd = accept(sockfd, (struct sockaddr *)&their_addr, &sin_size)) == -1) {
                    perror("accept");
                    continue;
                }

                for( i = 0 ; i < MAXCLIENTS ; i ++ ) {
                    if ( clients[i] == 0 ) {
                        clients[i] = new_fd ;
                        break;
                    }
                }

                //check if full
                if ( i != MAXCLIENTS ) {
                    if ( new_fd > highest ) {
                        highest = clients[i] ;
                    }
                    // printf("Connexion received from %s (slot %i)",inet_ntoa(their_addr.sin_addr),i);
                    // send(new_fd,"Welcome !",10,MSG_NOSIGNAL);
                }
                else {
                    // send(new_fd, "No room for you !",18,MSG_NOSIGNAL);
                    close(new_fd);
                }
            }

            //read client data
            for ( i = 0 ; i < MAXCLIENTS ; i ++ ) {
                if ( FD_ISSET(clients[i],&readfds) ) {
                    if ( (numbytes=recv(clients[i],buffer,MAXDATASIZE,0)) <= 0 ) {
                        printf("Connexion lost from slot %i",i);
                        close(clients[i]);
                        clients[i] = 0 ;
                    }
                    else {
                        buffer[numbytes] = '\0';
                        //printf("Received from slot %i : %s",i,buffer);
                    }
                }
            }
        }
        else {
            perror("select");
            continue;
        }
    }
    return 0;
}
