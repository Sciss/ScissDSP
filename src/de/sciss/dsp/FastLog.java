/*
 *  FastLog.java
 *  (ScissDSP)
 *
 *  Copyright (c) 2001-2010 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 *		22-Apr-08	created
 *		27-Jan-10	copied from EisK
 */

package de.sciss.dsp;

/**
 * 	Implementation of the ICSILog algorithm
 *	as described in O. Vinyals, G. Friedland, N. Mirghafori
 *	"Revisiting a basic function on current CPUs: A fast logarithm implementation
 *	with adjustable accuracy" (2007).
 *
 *	@author		Hanns Holger Rutz
 *	@version	0.70, 22-Apr-08
 *
 *	@see		java.lang.Float#floatToRawIntBits( float )
 */
public class FastLog
{
	private final int		q, qM1;
	private final float[]	data;
	private float			korr;

	/**
	 * 	Create a new logarithm calculation instance. This will
	 * 	hold the pre-calculated log values for a given base
	 * 	and a table size depending on a given mantissa quantization.
	 * 
	 *	@param	base	the logarithm base (e.g. 2 for log duals, 10 for
	 *					decibels calculations, Math.E for natural log)
	 *	@param	q		the quantization, the number of bits to remove
	 *					from the mantissa. for q = 11, the table storage
	 *					requires 32 KB.
	 */
	public FastLog( double base, int q )
	{
		final int tabSize = 1 << (24 - q);

		this.q	= q;
		qM1		= q - 1;
		korr	= (float) (MathUtil.LN2 / Math.log( base ));
		data	= new float[ tabSize ];
		
		for( int i = 0; i < tabSize; i++ ) {
			// note: the -150 is to avoid this addition in the calculation
			// of the exponent (see the floatToRawIntBits doc).
			data[ i ] = (float) (MathUtil.log2( i << q ) - 150);
		}
	}
	
	/**
	 *	Calculate the logarithm to the base given in the constructor.
	 *
	 *	@param	x	the argument. must be positive!
	 *	@return		log( x )
	 */
	public float calc( float x )
	{
//		final int raw	= Float.floatToRawIntBits( x );
		final int raw	= Float.floatToIntBits( x );
		final int exp	= (raw >> 23) & 0xFF;
		final int mant	= (raw & 0x7FFFFF);

		return (exp + data[ exp == 0 ?
			(mant >> qM1) :
			((mant | 0x800000) >> q) ]) * korr;
	}
}
