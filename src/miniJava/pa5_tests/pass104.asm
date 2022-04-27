  0         LOADL        0
  1         CALL         newarr  
  2         CALL         L10
  3         HALT   (0)   
  4  L10:   LOADL        0
  5         LOADL        10
  6         CALL         newarr  
  7         LOADL        1
  8         LOAD         3[LB]
  9         JUMP         L12
 10  L11:   LOAD         6[LB]
 11         CALL         putintnl
 12         POP          0
 13         LOAD         6[LB]
 14         LOAD         5[LB]
 15         CALL         add     
 16         STORE        6[LB]
 17  L12:   LOAD         6[LB]
 18         LOAD         4[LB]
 19         CALL         arraylen
 20         CALL         lt      
 21         JUMPIF (1)   L11
 22         POP          1
 23         RETURN (0)   1
