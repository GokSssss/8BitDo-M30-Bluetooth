#include <linux/uinput.h>
#include <linux/input.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
static void event(int fd,int type,int code,int value){struct input_event e={0};gettimeofday(&e.time,0);e.type=type;e.code=code;e.value=value;write(fd,&e,sizeof(e));}
int main(){
 int fd=open("/dev/uinput",O_WRONLY|O_NONBLOCK);if(fd<0){perror("open uinput");return 1;}
 ioctl(fd,UI_SET_EVBIT,EV_KEY);int keys[]={BTN_A,BTN_B,BTN_X,BTN_Y,BTN_TL,BTN_TR,BTN_TL2,BTN_TR2,BTN_START,BTN_SELECT,BTN_MODE};
 for(unsigned i=0;i<sizeof(keys)/sizeof(int);i++)ioctl(fd,UI_SET_KEYBIT,keys[i]);
 ioctl(fd,UI_SET_EVBIT,EV_ABS);ioctl(fd,UI_SET_ABSBIT,ABS_X);ioctl(fd,UI_SET_ABSBIT,ABS_Y);ioctl(fd,UI_SET_ABSBIT,ABS_HAT0X);ioctl(fd,UI_SET_ABSBIT,ABS_HAT0Y);
 struct uinput_user_dev dev={0};strcpy(dev.name,"M30 Virtual Test");dev.id.bustype=BUS_BLUETOOTH;dev.id.vendor=0x2dc8;dev.id.product=0x6100;dev.id.version=1;
 dev.absmin[ABS_X]=dev.absmin[ABS_Y]=-32768;dev.absmax[ABS_X]=dev.absmax[ABS_Y]=32767;
 dev.absmin[ABS_HAT0X]=dev.absmin[ABS_HAT0Y]=-1;dev.absmax[ABS_HAT0X]=dev.absmax[ABS_HAT0Y]=1;
 write(fd,&dev,sizeof(dev));if(ioctl(fd,UI_DEV_CREATE)<0){perror("create");return 2;}
 puts("virtual gamepad ready");fflush(stdout);
 for(;;){FILE* f=fopen("/data/local/tmp/m30-command","r");if(f){int type,code,value;if(fscanf(f,"%d %d %d",&type,&code,&value)==3){if(type<0){fclose(f);unlink("/data/local/tmp/m30-command");break;}event(fd,type,code,value);event(fd,EV_SYN,SYN_REPORT,0);}fclose(f);unlink("/data/local/tmp/m30-command");}usleep(50000);}
 ioctl(fd,UI_DEV_DESTROY);close(fd);return 0;
}
