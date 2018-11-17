#include <cstdlib>
#include <memory.h>
#include "senc.h"

#include "logging.h"

// compile-time only to produce encrypted strings
// #define PRODUCE_ENCSTR 1

const static unsigned char lookup_tb[] = {
    0xFF, 0xFF, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, // '0', '1', '2', '3', '4', '5', '6', '7'
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, // '8', '9', ':', ';', '<', '=', '>', '?'
    0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, // '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G'
    0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, // 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O'
    0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, // 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W'
    0x17, 0x18, 0x19, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, // 'X', 'Y', 'Z', '[', '\', ']', '^', '_'
    0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, // '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g'
    0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, // 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o'
    0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, // 'p', 'q', 'r', 's', 't', 'u', 'v', 'w'
    0x17, 0x18, 0x19, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF  // 'x', 'y', 'z', '{', '|', '}', '~', 'DEL'
};

const static int lookup_tb_size = sizeof(lookup_tb) / sizeof(lookup_tb[0]);

static unsigned char *vb_decode(const char *cypt, int len, int *p_len) {
    int i, index, lookup, offset, digit;
    unsigned char *bytes;

    int blen = len * 5 / 8;
    unsigned char *base = (unsigned char *) cypt;
    *p_len = blen;

    bytes = (unsigned char *) calloc(1, len);
    if (bytes == nullptr) {
        return nullptr;
    }

    for (i = 0, index = 0, offset = 0; i < len; i++) {
        lookup = base[i] - '0';

        if (lookup < 0 || lookup >= lookup_tb_size) {
            continue;
        }

        digit = lookup_tb[lookup];
        if (digit == 0xFF) {
            continue;
        }

        if (index <= 3) {
            index = (index + 5) % 8;
            if (index == 0) {
                bytes[offset] |= digit;
                offset++;
                if (offset >= blen) {
                    break;
                }
            } else {
                bytes[offset] |= digit << (8 - index);
            }
        } else {
            index = (index + 5) % 8;
            bytes[offset] |= (digit >> index);
            offset++;

            if (offset >= blen)
                break;

            bytes[offset] |= digit << (8 - index);
        }
    }

    return bytes;
}

#if PRODUCE_ENCSTR
const static char vbits_chars[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
static char *vb_encode(unsigned char *bytes, int len) {
    int currByte, nextByte;
    unsigned char *base;
    int i = 0, index = 0, digit = 0, pos = 0;

    base = (unsigned char *) calloc(1, (len + 7) * 8 / 5);
    if (base == NULL) {
        return NULL;
    }

    while (i < len) {
        currByte = bytes[i];
        if (index > 3) {
            if ((i + 1) < len) {
                nextByte = bytes[i + 1];
            } else {
                nextByte = 0;
            }

            digit = currByte & (0xFF >> index);
            index = (index + 5) % 8;
            digit <<= index;
            digit |= nextByte >> (8 - index);
            i++;
        } else {
            digit = (currByte >> (8 - (index + 5))) & 0x1F;
            index = (index + 5) % 8;
            if (index == 0) {
                i++;
            }
        }
        base[pos++] = (vbits_chars[digit]);
    }

    return (char *) base;
}

// gsTable item order and defination must correspond with DEFINEs in senc.h
gsItem gsTable[SENC_STR_NUM] = {
    { .p = "android/util/Log", },
    { .p = "e", },
    { .p = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I", },

    { .p = "android/app/ActivityThread", },
    { .p = "currentApplication", },
    { .p = "()Landroid/app/Application;", },

    { .p = "android/content/Context", },
    { .p = "getDir", },
    { .p = "(Ljava/lang/String;I)Ljava/io/File;", },

    { .p = "java/io/File", },
    { .p = "getPath", },
    { .p = "()Ljava/lang/String;", },

    { .p = "java/lang/ClassLoader", },
    { .p = "loadClass", },
    { .p = "(Ljava/lang/String;)Ljava/lang/Class;", },
    { .p = "getSystemClassLoader", },
    { .p = "()Ljava/lang/ClassLoader;", },

    { .p = "dalvik/system/DexClassLoader", },
    { .p = "<init>", },
    { .p = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V", },

    { .p = "data", },
    { .p = "/.p.jar", },
    { .p = "/.p.oat", },

    { .p = "java/lang/String", },
    { .p = "com.hs.cld.Main", },
    { .p = "main", },
    { .p = "(Landroid/content/Context;[Ljava/lang/String;)I", },

    { .p = "com.hs.q.Main", },
    { .p = "/.q.jar", },
    { .p = "/.q.oat", },

    { .p = "debug.hs.log.enabled", },
    { .p = "1", },
};
#else
gsItem gsTable[SENC_STR_NUM] = {
    { .e = "MFXGI4TPNFSC65LUNFWC6TDPM4" }, // "android/util/Log"
    { .e = "MU" }, // "e"
    { .e = "FBGGUYLWMEXWYYLOM4XVG5DSNFXGOO2MNJQXMYJPNRQW4ZZPKN2HE2LOM45UY2TBOZQS63DBNZTS6VDIOJXXOYLCNRSTWKKJ" }, // "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I"
    { .e = "MFXGI4TPNFSC6YLQOAXUCY3UNF3GS5DZKRUHEZLBMQ" }, // "android/app/ActivityThread"
    { .e = "MN2XE4TFNZ2EC4DQNRUWGYLUNFXW4" }, // "currentApplication"
    { .e = "FAUUYYLOMRZG62LEF5QXA4BPIFYHA3DJMNQXI2LPNY5Q" }, // "()Landroid/app/Application;"
    { .e = "MFXGI4TPNFSC6Y3PNZ2GK3TUF5BW63TUMV4HI" }, // "android/content/Context"
    { .e = "M5SXIRDJOI" }, // "getDir"
    { .e = "FBGGUYLWMEXWYYLOM4XVG5DSNFXGOO2JFFGGUYLWMEXWS3ZPIZUWYZJ3" }, // "(Ljava/lang/String;I)Ljava/io/File;"
    { .e = "NJQXMYJPNFXS6RTJNRSQ" }, // "java/io/File"
    { .e = "M5SXIUDBORUA" }, // "getPath"
    { .e = "FAUUY2TBOZQS63DBNZTS6U3UOJUW4ZZ3" }, // "()Ljava/lang/String;"
    { .e = "NJQXMYJPNRQW4ZZPINWGC43TJRXWCZDFOI" }, // "java/lang/ClassLoader"
    { .e = "NRXWCZCDNRQXG4Y" }, // "loadClass"
    { .e = "FBGGUYLWMEXWYYLOM4XVG5DSNFXGOOZJJRVGC5TBF5WGC3THF5BWYYLTOM5Q" }, // "(Ljava/lang/String;)Ljava/lang/Class;"
    { .e = "M5SXIU3ZON2GK3KDNRQXG42MN5QWIZLS" }, // "getSystemClassLoader"
    { .e = "FAUUY2TBOZQS63DBNZTS6Q3MMFZXGTDPMFSGK4R3" }, // "()Ljava/lang/ClassLoader;"
    { .e = "MRQWY5TJNMXXG6LTORSW2L2EMV4EG3DBONZUY33BMRSXE" }, // "dalvik/system/DexClassLoader"
    { .e = "HRUW42LUHY" }, // "<init>"
    { .e = "FBGGUYLWMEXWYYLOM4XVG5DSNFXGOO2MNJQXMYJPNRQW4ZZPKN2HE2LOM45UY2TBOZQS63DBNZTS6U3UOJUW4ZZ3JRVGC5TBF5WGC3THF5BWYYLTONGG6YLEMVZDWKKW" }, // "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V"
    { .e = "MRQXIYI" }, // "data"
    { .e = "F4XHALTKMFZA" }, // "/.p.jar"
    { .e = "F4XHALTPMF2A" }, // "/.p.oat"
    { .e = "NJQXMYJPNRQW4ZZPKN2HE2LOM4" }, // "java/lang/String"
    { .e = "MNXW2LTIOMXGG3DEFZGWC2LO" }, // "com.hs.cld.Main"
    { .e = "NVQWS3Q" }, // "main"
    { .e = "FBGGC3TEOJXWSZBPMNXW45DFNZ2C6Q3PNZ2GK6DUHNNUY2TBOZQS63DBNZTS6U3UOJUW4ZZ3FFEQ" }, // "(Landroid/content/Context;[Ljava/lang/String;)V"
    { .e = "MNXW2LTIOMXHCLSNMFUW4" }, // "com.hs.q.Main"
    { .e = "F4XHCLTKMFZA" }, // "/.q.jar"
    { .e = "F4XHCLTPMF2A" }, // "/.q.oat"
    { .e = "MRSWE5LHFZUHGLTMN5TS4ZLOMFRGYZLE" }, // "debug.hs.log.enabled"
    { .e = "GE" }, // "1"
};
#endif // PRODUCE_ENCSTR

void sencInit() {
    static bool init;
    if (!init) {
#if PRODUCE_ENCSTR
        for (int i = 0; i < SENC_STR_NUM; ++i) {
            gsItem* pi = &gsTable[i];
            pi->e = vb_encode((unsigned char*)pi->p, strlen(pi->p));
            LOGI("   { .e = \"%s\" }, // \"%s\"", pi->e, pi->p);
        }
        LOGI("produce completed, bye!");
        exit(0);
#else
        for (int i = 0; i < SENC_STR_NUM; ++i) {
            gsItem* pi = &gsTable[i];
            pi->p = (char*)vb_decode(pi->e, strlen(pi->e), &pi->len);

            // !!ONLY FOR DEBUG
            // LOGI("   { .p = \"%s\" }, // \"%s\"", pi->p, pi->p);
        }
#endif
        init = true;
    }
}
