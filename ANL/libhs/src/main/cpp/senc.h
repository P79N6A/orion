
#ifndef ANL_SENC_H
#define ANL_SENC_H

#define SENC_CLS_LOG          0
#define SENC_LOG_E_MID        (SENC_CLS_LOG + 1)
#define SENC_LOG_E_SIG        (SENC_LOG_E_MID + 1)

#define SENC_CLS_AT           (SENC_LOG_E_SIG + 1)
#define SENC_AT_C_MID         (SENC_CLS_AT + 1)
#define SENC_AT_C_SIG         (SENC_AT_C_MID + 1)

#define SENC_CLS_CTX          (SENC_AT_C_SIG + 1)
#define SENC_CTX_G_MID        (SENC_CLS_CTX + 1)
#define SENC_CTX_G_SIG        (SENC_CTX_G_MID + 1)

#define SENC_CLS_FILE         (SENC_CTX_G_SIG + 1)
#define SENC_FILE_G_MID       (SENC_CLS_FILE + 1)
#define SENC_FILE_G_SIG       (SENC_FILE_G_MID + 1)

#define SENC_CLS_CLSLDR       (SENC_FILE_G_SIG + 1)
#define SENC_CLSLDR_L_MID     (SENC_CLS_CLSLDR + 1)
#define SENC_CLSLDR_L_SIG     (SENC_CLSLDR_L_MID + 1)

#define SENC_CLSLDR_G_MID     (SENC_CLSLDR_L_SIG + 1)
#define SENC_CLSLDR_G_SIG     (SENC_CLSLDR_G_MID + 1)

#define SENC_CLS_DXCLSLDR     (SENC_CLSLDR_G_SIG + 1)
#define SENC_DXCLSLDR_I_MID   (SENC_CLS_DXCLSLDR + 1)
#define SENC_DXCLSLDR_I_SIG   (SENC_DXCLSLDR_I_MID + 1)

#define SENC_DEXPATH          (SENC_DXCLSLDR_I_SIG + 1)
#define SENC_DEXFNAME         (SENC_DEXPATH + 1)
#define SENC_ODEXFNAME        (SENC_DEXFNAME + 1)
#define SENC_CLS_STRING       (SENC_ODEXFNAME + 1)

#define SENC_CLS_CLDMAIN      (SENC_CLS_STRING + 1)
#define SENC_CLDMAIN_M_MID    (SENC_CLS_CLDMAIN + 1)
#define SENC_CLDMAIN_M_SIG    (SENC_CLDMAIN_M_MID + 1)

#define SENC_CLS_QMAIN        (SENC_CLDMAIN_M_SIG + 1)
#define SENC_Q_DEXFNAME       (SENC_CLS_QMAIN + 1)
#define SENC_Q_ODEXFNAME      (SENC_Q_DEXFNAME + 1)

#define SENC_K_LE             (SENC_Q_ODEXFNAME + 1)
#define SENC_V_1              (SENC_K_LE + 1)

#define SENC_STR_NUM          (SENC_V_1 + 1)

typedef struct gsItem_ {
    int len;
    const char* e;
    const char* p;
} gsItem;

extern gsItem gsTable[SENC_STR_NUM];

#ifndef _SGP
#undef  _SGP
#endif
#define _SGP(I) (gsTable[I].p)

void sencInit();

#endif // ANL_SENC_H
