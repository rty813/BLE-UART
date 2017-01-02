#include "timer.h"
#include "led.h"

void TIMER_INIT(u16 ARR, u16 PSC)
{
	NVIC_InitTypeDef NVIC_Str;	
	TIM_ICInitTypeDef  TIM3_ICInitStructure;
	TIM_TimeBaseInitTypeDef TimerInitStr;
	
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_TIM3,ENABLE);
	

	
	TimerInitStr.TIM_CounterMode = TIM_CounterMode_Up;
	TimerInitStr.TIM_Prescaler = PSC;
	TimerInitStr.TIM_Period = ARR;	
	TIM_TimeBaseInit(TIM3,&TimerInitStr);
	
	TIM3_ICInitStructure.TIM_Channel = TIM_Channel_1; //CC1S=01 	选择输入端 IC1映射到TI1上
  	TIM3_ICInitStructure.TIM_ICPolarity = TIM_ICPolarity_Rising;	//上升沿捕获
  	TIM3_ICInitStructure.TIM_ICSelection = TIM_ICSelection_DirectTI; //映射到TI1上
  	TIM3_ICInitStructure.TIM_ICPrescaler = TIM_ICPSC_DIV1;	 //配置输入分频,不分频 
  	TIM3_ICInitStructure.TIM_ICFilter = 0x0a;//IC1F=0000 配置输入滤波器 不滤波
	
  	TIM_ICInit(TIM3, &TIM3_ICInitStructure);

	NVIC_Str.NVIC_IRQChannel =   TIM3_IRQn;
	NVIC_Str.NVIC_IRQChannelPreemptionPriority = 1;
	NVIC_Str.NVIC_IRQChannelSubPriority = 2;
	NVIC_Str.NVIC_IRQChannelCmd = ENABLE;
	
	NVIC_Init(&NVIC_Str);
	
	TIM_ITConfig(TIM3,TIM_IT_Update|TIM_IT_CC1,ENABLE);
	TIM_Cmd(TIM3,ENABLE);
		
}

u8  TIM3CH1_CAPTURE_STA=0;	
u16	TIM3CH1_CAPTURE_VAL;	

void TIM3_IRQHandler(void)
{

 	if((TIM3CH1_CAPTURE_STA&0X80)==0)//还未成功捕获	
	{	  
		if (TIM_GetITStatus(TIM3, TIM_IT_Update) != RESET)
		 
		{	    
			if(TIM3CH1_CAPTURE_STA&0X40)//已经捕获到高电平了
			{
				if((TIM3CH1_CAPTURE_STA&0X3F)==0X3F)//高电平太长了
				{
					TIM3CH1_CAPTURE_STA|=0X80;//标记成功捕获了一次
					TIM3CH1_CAPTURE_VAL=0XFFFF;
				}else TIM3CH1_CAPTURE_STA++;
			}	 
		}
	if (TIM_GetITStatus(TIM3, TIM_IT_CC1) != RESET)//捕获1发生捕获事件
		{	
			if(TIM3CH1_CAPTURE_STA&0X40)		//捕获到一个下降沿 		
			{	  			
				TIM3CH1_CAPTURE_STA|=0X80;		//标记成功捕获到一次高电平脉宽
				TIM3CH1_CAPTURE_VAL=TIM_GetCapture1(TIM3);
		   		TIM_OC1PolarityConfig(TIM3,TIM_ICPolarity_Rising); //CC1P=0 设置为上升沿捕获
			}else  								//还未开始,第一次捕获上升沿
			{
				TIM3CH1_CAPTURE_STA=0;			//清空
				TIM3CH1_CAPTURE_VAL=0;
	 			TIM_SetCounter(TIM3,0);
				TIM3CH1_CAPTURE_STA|=0X40;		//标记捕获到了上升沿
		   		TIM_OC1PolarityConfig(TIM3,TIM_ICPolarity_Falling);		//CC1P=1 设置为下降沿捕获
			}		    
		}			     	    					   
 	}
 
    TIM_ClearITPendingBit(TIM3, TIM_IT_CC1|TIM_IT_Update); //清除中断标志位

}

