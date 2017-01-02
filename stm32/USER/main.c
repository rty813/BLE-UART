#include "sys.h"
#include "delay.h"
#include "usart.h"
#include "led.h"
#include "timer.h"

extern u8  TIM3CH1_CAPTURE_STA;		//输入捕获状态		    				
extern u16	TIM3CH1_CAPTURE_VAL;	//输入捕获值	

int main(void)
{	
	u32 temp=0; 
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);
	delay_init();	    //延时函数初始化	  
	LED_Init();		  	//初始化与LED连接的硬件接口
	uart_init(115200);	 //串口初始化为115200;
	printf("我爱姜姜！");
	
	while(1)
	{
		if (USART_RX_STA & 0x8000){
			if (USART_RX_BUF[0] == '1'){
				led = !led;
				ledLove = !ledLove;
			}
			USART_RX_STA = 0;
		}
		delay_ms(10);
	}
}
