#include "sys.h"
#include "delay.h"
#include "usart.h"
#include "led.h"
#include "timer.h"

extern u8  TIM3CH1_CAPTURE_STA;		//���벶��״̬		    				
extern u16	TIM3CH1_CAPTURE_VAL;	//���벶��ֵ	

int main(void)
{	
	u32 temp=0; 
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);
	delay_init();	    //��ʱ������ʼ��	  
	LED_Init();		  	//��ʼ����LED���ӵ�Ӳ���ӿ�
	uart_init(115200);	 //���ڳ�ʼ��Ϊ115200;
	printf("�Ұ�������");
	
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
