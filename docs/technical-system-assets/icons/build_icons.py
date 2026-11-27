"""Build original diagram icons as editable SVG and transparent PNG.

Only Pillow is required. All geometry is local; no downloaded assets or fonts.
Run from this directory with: python build_icons.py
"""
from pathlib import Path
from math import cos, sin, pi
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent
INK = '#24394D'
SCALE = 4


class Icon:
    def __init__(self, name):
        self.name = name
        self.im = Image.new('RGBA', (128*SCALE, 128*SCALE))
        self.d = ImageDraw.Draw(self.im)
        self.svg = []

    def rect(self, xy, fill, radius=6, stroke=INK, width=3):
        x, y, xx, yy = xy
        self.d.rounded_rectangle(tuple(round(v*SCALE) for v in xy), radius*SCALE,
                                 fill=fill, outline=stroke, width=width*SCALE)
        self.svg.append(f'<rect x="{x}" y="{y}" width="{xx-x}" height="{yy-y}" rx="{radius}" fill="{fill}" stroke="{stroke}" stroke-width="{width}"/>')

    def ellipse(self, xy, fill, stroke=INK, width=3):
        x, y, xx, yy = xy
        self.d.ellipse(tuple(round(v*SCALE) for v in xy), fill=fill,
                       outline=stroke, width=width*SCALE)
        self.svg.append(f'<ellipse cx="{(x+xx)/2}" cy="{(y+yy)/2}" rx="{(xx-x)/2}" ry="{(yy-y)/2}" fill="{fill}" stroke="{stroke}" stroke-width="{width}"/>')

    def line(self, pts, color=INK, width=3):
        self.d.line([(round(x*SCALE), round(y*SCALE)) for x, y in pts], fill=color,
                    width=width*SCALE, joint='curve')
        self.svg.append(f'<polyline points="{" ".join(f"{x},{y}" for x,y in pts)}" fill="none" stroke="{color}" stroke-width="{width}" stroke-linejoin="round" stroke-linecap="round"/>')

    def polygon(self, pts, fill, stroke=INK, width=3):
        self.d.polygon([(round(x*SCALE), round(y*SCALE)) for x,y in pts], fill=fill)
        self.line(pts+[pts[0]], stroke, width)
        self.svg.append(f'<polygon points="{" ".join(f"{x},{y}" for x,y in pts)}" fill="{fill}" stroke="{stroke}" stroke-width="{width}" stroke-linejoin="round"/>')

    def save(self):
        self.im.resize((128,128), Image.Resampling.LANCZOS).save(OUT/(self.name+'.png'))
        (OUT/(self.name+'.svg')).write_text('<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 128 128">'+''.join(self.svg)+'</svg>\n')


def ground(i):
    i.ellipse((17,111,112,120),'#E9EEF4','#E9EEF4',1)


def db(i, x=22, y=23, w=83, h=80, fill='#B9D9F5', top='#E1EFFB'):
    i.rect((x,y+12,x+w,y+h-10),fill,1)
    i.ellipse((x,y+h-22,x+w,y+h),fill)
    i.rect((x+3,y+16,x+w-3,y+h-12),fill,0,fill,1)
    for yy in [y+27,y+48]:
        i.ellipse((x,yy,x+w,yy+22),fill)
        i.rect((x+3,yy-2,x+w-3,yy+10),fill,0,fill,1)
    i.ellipse((x,y,x+w,y+24),top)


def ticket(i, x=16, y=30, fill='#B5E5DB'):
    i.rect((x+6,y-5,x+99,y+60),'#E7F5F1',8,'#309985',3)
    i.rect((x,y,x+93,y+65),fill,8)
    for yy in range(y+8,y+57,12): i.line([(x+64,yy),(x+64,yy+5)],'#4E8C82',2)
    i.ellipse((x-5,y+24,x+9,y+40),'white','white',1)
    i.ellipse((x+84,y+24,x+98,y+40),'white','white',1)


def envelope(i, fill='#FFE3A4'):
    i.rect((13,34,115,103),fill,8)
    i.polygon([(15,38),(64,77),(113,38)],'#FFF4D8')
    i.line([(17,98),(48,72)],'#B98B32',3)
    i.line([(111,98),(80,72)],'#B98B32',3)


def check(i, x=64, y=74, fill='#1B9F83'):
    i.ellipse((x,y,x+42,y+42),fill)
    i.line([(x+10,y+22),(x+18,y+30),(x+33,y+13)],'white',5)


def gear(i, x=64, y=64, r=32, fill='#BBD9F6'):
    pts=[]
    for j in range(48):
        rr=r if j%6 in (0,1,2) else r-8
        a=2*pi*j/48;pts.append((round(x+rr*cos(a),2),round(y+rr*sin(a),2)))
    i.polygon(pts,fill)
    i.ellipse((x-13,y-13,x+13,y+13),'white')


def build():
    i=Icon('frontend');ground(i)
    i.rect((12,17,116,99),'white',10)
    i.rect((15,20,113,39),'#D3E7FA',5,'#D3E7FA',1)
    for x,c in [(25,'#E9878F'),(39,'#F2C45C'),(53,'#57BAA3')]:i.ellipse((x,25,x+7,32),c,c,1)
    i.rect((24,50,61,84),'#BBDDF4',4,'#4388B8',2)
    i.line([(71,55),(102,55)],'#779BB6',4);i.line([(71,67),(102,67)],'#779BB6',4)
    i.rect((72,77,100,87),'#61B6A1',3,'#61B6A1',1)
    i.line([(52,100),(52,112),(76,112),(76,100)],INK,3);i.line([(39,113),(89,113)],INK,4);i.save()

    i=Icon('gateway');ground(i)
    i.polygon([(12,30),(40,16),(40,108),(12,96)],'#A596DE')
    i.polygon([(88,16),(116,30),(116,96),(88,108)],'#C8BDEB')
    i.rect((42,34,86,91),'#7B5AC4',5)
    i.line([(51,56),(44,64),(51,72)],'white',4)
    i.line([(77,56),(84,64),(77,72)],'white',4)
    i.line([(68,50),(59,79)],'white',4)
    i.line([(6,64),(28,64)],'#5E429C',5);i.line([(100,64),(122,64)],'#5E429C',5)
    i.line([(115,57),(123,64),(115,71)],'#5E429C',4);i.save()

    i=Icon('identity');ground(i)
    i.polygon([(64,12),(105,29),(101,72),(88,96),(64,113),(40,96),(27,72),(23,29)],'#D5CBF0')
    i.ellipse((43,33,76,66),'#A58ADB')
    i.rect((58,62,69,92),'#8562BD',3)
    i.line([(66,82),(80,82),(80,92)],'#8562BD',5);i.save()

    i=Icon('organization');ground(i)
    i.rect((12,47,49,110),'#DAD2EF',4)
    i.rect((48,17,109,110),'#B9A9DF',6)
    for x in [59,82]:
        for y in [30,52,74]:i.rect((x,y,x+13,y+12),'#F6F1FF',2,INK,2)
    for y in [60,81]:i.rect((22,y,36,y+12),'#F6F1FF',2,INK,2)
    i.rect((69,94,89,111),'#F6F1FF',2);i.save()

    i=Icon('commerce');ground(i);ticket(i,fill='#B7DDF5')
    i.polygon([(46,44),(51,55),(63,57),(54,66),(56,79),(46,73),(35,79),(37,66),(28,57),(40,55)],'#FFD46B',INK,2);i.save()

    i=Icon('payment');ground(i)
    i.rect((14,22,113,88),'#8EC7EC',9)
    i.rect((17,37,110,52),'#305F84',0,'#305F84',1)
    i.rect((26,62,45,76),'#F5D273',3,INK,2)
    i.line([(57,70),(77,70)],'white',4)
    for y in [97,89,81]:i.ellipse((67,y,115,y+19),'#F1C45D',INK,2)
    i.ellipse((67,77,115,96),'#FFE39A',INK,2);i.line([(91,82),(91,91)],'#B88424',3);i.save()

    i=Icon('admission');ground(i);ticket(i,fill='#A9DFD1')
    for x,y in [(29,42),(49,42),(29,63)]:
        i.rect((x,y,x+14,y+14),INK,0,INK,1);i.rect((x+4,y+4,x+10,y+10),'#EEFFF9',0,'#EEFFF9',1)
    i.rect((51,66,59,74),INK,0,INK,1);check(i,79,77);i.save()

    i=Icon('finance');ground(i)
    i.rect((19,13,101,108),'#F3F8FD',6)
    i.line([(33,28),(86,28)],'#98B9D4',4)
    for x,y,c in [(33,77,'#8EC2E6'),(50,64,'#53A8CF'),(67,48,'#237DB7')]:i.rect((x,y,x+12,92),c,2,c,1)
    i.line([(30,94),(84,94)],INK,2);i.ellipse((79,78,116,115),'#F6D276');i.line([(97,87),(97,106)],'#AC7F2F',4);i.save()

    i=Icon('discovery');ground(i)
    i.rect((13,25,88,98),'#D1EFE9',8)
    i.line([(27,80),(41,62),(55,70),(76,43)],'#258F80',5)
    i.ellipse((52,18,104,70),'#B8E3DE')
    i.ellipse((61,27,95,61),'#F3FFFD',INK,2)
    i.line([(97,64),(117,88)],INK,12);i.line([(98,65),(116,86)],'#59B8AC',6);i.save()

    i=Icon('notification');ground(i);envelope(i,'#FADCA1')
    i.ellipse((82,10,115,43),'#E9A34A');i.ellipse((94,18,102,26),'white','white',1)
    i.line([(97,26),(97,34)],'white',4);i.save()

    i=Icon('admin');ground(i)
    i.rect((14,18,113,108),'#D1ECE6',9)
    for x in [36,64,92]:i.line([(x,32),(x,94)],'#337F76',4)
    for x,y in [(36,48),(64,77),(92,57)]:i.rect((x-9,y-7,x+9,y+7),'white',4,INK,2)
    i.save()

    i=Icon('postgresql');ground(i);db(i);i.ellipse((83,83,117,117),'#367FA8');i.line([(92,101),(99,108),(109,92)],'white',4);i.save()

    i=Icon('kafka');ground(i)
    for x,y in [(33,25),(95,25),(105,83),(62,107),(18,78)]:
        i.line([(62,62),(x,y)],'#BD7B2B',4);i.ellipse((x-11,y-11,x+11,y+11),'#F6D785')
    i.ellipse((40,40,84,84),'#ECAB48');i.ellipse((53,53,71,71),'white',INK,2);i.save()

    i=Icon('minio');ground(i)
    for x,y in [(12,57),(60,57),(36,17)]:
        i.polygon([(x,y+13),(x+21,y),(x+44,y+13),(x+22,y+27)],'#F3BBC5')
        i.polygon([(x,y+13),(x+22,y+27),(x+22,y+58),(x,y+44)],'#DC859A')
        i.polygon([(x+22,y+27),(x+44,y+13),(x+44,y+44),(x+22,y+58)],'#F6D5DD')
    i.save()

    i=Icon('smtp');ground(i);envelope(i)
    i.polygon([(64,26),(115,10),(103,58),(88,39)],'#C3E6F7')
    i.line([(88,39),(109,17)],'#3F91B6',3);i.save()

    i=Icon('controller');ground(i)
    i.rect((12,21,115,106),'#E9F2FA',8)
    i.rect((16,25,111,42),'#BBD9EE',4,'#BBD9EE',1)
    i.line([(44,58),(31,71),(44,84)],'#247BAA',5)
    i.line([(83,58),(96,71),(83,84)],'#247BAA',5)
    i.line([(71,53),(57,90)],'#247BAA',5);i.save()

    i=Icon('order-service');ground(i);gear(i,64,64,48,'#F3CB73');i.save()
    i=Icon('inventory');ground(i);ticket(i,fill='#B8E3D6');i.line([(30,61),(54,61)],'#248E78',4);i.line([(42,49),(42,73)],'#248E78',4);i.save()
    i=Icon('repository');ground(i);db(i,fill='#C5DCEF');i.save()
    i=Icon('facts');ground(i)
    i.rect((23,16,103,110),'#FFE8AF',7)
    for y in [36,58,80]:
        i.line([(36,y),(43,y+6),(52,y-7)],'#B98525',3);i.line([(61,y),(88,y)],'#AB8546',4)
    i.save()
    i=Icon('worker');ground(i)
    i.rect((15,25,112,100),'#E5DDF5',8)
    gear(i,64,63,30,'#B8A6D9')
    i.line([(28,111),(101,111)],'#8566AD',4);i.save()


if __name__ == '__main__':
    build()
    print('Built 21 original SVG/PNG icon pairs in', OUT)
