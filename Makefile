# $Id: DebianMakefile,v 1.1 2006-04-07 13:06:48 ctl Exp $
# Installs 3dm on a Debian system (change paths for other Linuxes)
# NOTE: Quick-and-dirty hack
TARGETDIR=/usr/local/3dm
BINDIR=/usr/local/bin
#TARGETDIR=tmp2
#BINDIR=tmp2
VERID=0.1.5beta1-custom
install:	
	ant contrib-get
	ant -D3dm.version=$(VERID) release
	install -d -m 0755 $(TARGETDIR)
	install -m 0755 build/3dm-$(VERID).jar $(TARGETDIR)/3dm.jar
	install -m 0755 3dm $(BINDIR) 
